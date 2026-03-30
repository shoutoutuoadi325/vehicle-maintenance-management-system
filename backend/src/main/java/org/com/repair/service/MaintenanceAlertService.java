package org.com.repair.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.com.repair.DTO.MaintenanceAlertPageResponse;
import org.com.repair.DTO.MaintenanceAlertResponse;
import org.com.repair.DTO.MaintenanceAlertSummaryResponse;
import org.com.repair.entity.MaintenanceAlert;
import org.com.repair.entity.MaintenanceAlert.AlertStatus;
import org.com.repair.entity.MaintenanceAlert.AlertType;
import org.com.repair.entity.Vehicle;
import org.com.repair.service.MaintenanceAlertRuleEvaluator.VehicleRiskSnapshot;
import org.com.repair.repository.MaintenanceAlertRepository;
import org.com.repair.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceAlertService {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceAlertService.class);
    private static final String ALERT_EVENT_SCAN_FAILURE = "MAINTENANCE_ALERT_SCAN_FAILURE";
    private static final String ALERT_EVENT_SCAN_SUMMARY = "MAINTENANCE_ALERT_SCAN_SUMMARY";
    private static final int ERROR_WARN_THRESHOLD = 1;

    private static final int MAINTENANCE_MILEAGE_THRESHOLD = 10000;
    private static final int UPCOMING_MILEAGE_WINDOW = 1000;
    private static final int UPCOMING_TIME_WINDOW_DAYS = 15;
    private static final Set<AlertStatus> UNRESOLVED_STATUSES = Set.of(AlertStatus.UNREAD, AlertStatus.READ);

    private final VehicleRepository vehicleRepository;
    private final MaintenanceAlertRepository maintenanceAlertRepository;
    private final MaintenanceAlertRuleEvaluator maintenanceAlertRuleEvaluator;

    public MaintenanceAlertService(VehicleRepository vehicleRepository,
                                   MaintenanceAlertRepository maintenanceAlertRepository,
                                   MaintenanceAlertRuleEvaluator maintenanceAlertRuleEvaluator) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceAlertRepository = maintenanceAlertRepository;
        this.maintenanceAlertRuleEvaluator = maintenanceAlertRuleEvaluator;
    }

    @Transactional
    @Scheduled(cron = "${maintenance.alert.scan-cron:0 0 2 * * *}")
    public void scanVehiclesForMaintenance() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        scanVehicles(vehicles, "daily-scan");
    }

    private boolean createAlertIfAbsent(Vehicle vehicle, AlertType alertType, String message) {
        boolean exists = maintenanceAlertRepository.existsByVehicleIdAndAlertTypeAndStatusIn(
                vehicle.getId(), alertType, UNRESOLVED_STATUSES);
        if (exists) {
            return false;
        }

        MaintenanceAlert alert = new MaintenanceAlert();
        alert.setUserId(vehicle.getUserId());
        alert.setVehicleId(vehicle.getId());
        alert.setAlertType(alertType);
        alert.setMessage(message);
        alert.setTriggerTime(LocalDateTime.now());
        alert.setStatus(AlertStatus.UNREAD);
        maintenanceAlertRepository.save(alert);
        return true;
    }

    @Transactional
    public List<MaintenanceAlertResponse> getUserAlerts(Long userId) {
        scanSingleUserVehicles(userId);
        return maintenanceAlertRepository.findByUserIdOrderByTriggerTimeDesc(userId)
                .stream()
                .map(MaintenanceAlertResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaintenanceAlertPageResponse getUserAlertsPage(Long userId,
                                                           int page,
                                                           int size,
                                                           String status,
                                                           String alertType) {
        scanSingleUserVehicles(userId);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);

        AlertStatus parsedStatus = parseStatus(status);
        AlertType parsedAlertType = parseAlertType(alertType);

        Page<MaintenanceAlert> resultPage = maintenanceAlertRepository.searchByUserId(
                userId, parsedStatus, parsedAlertType, pageRequest);

        return new MaintenanceAlertPageResponse(
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.getContent().stream().map(MaintenanceAlertResponse::new).collect(Collectors.toList())
        );
    }

    @Transactional
    public MaintenanceAlertSummaryResponse getUserSummary(Long userId) {
        scanSingleUserVehicles(userId);
        long unreadCount = maintenanceAlertRepository.countByUserIdAndStatus(userId, AlertStatus.UNREAD);

        List<Vehicle> vehicles = vehicleRepository.findByUserId(userId);
        VehicleRiskSnapshot riskSnapshot = maintenanceAlertRuleEvaluator.evaluateVehicleRisk(
            vehicles,
            LocalDateTime.now(),
            MAINTENANCE_MILEAGE_THRESHOLD,
            UPCOMING_MILEAGE_WINDOW,
            UPCOMING_TIME_WINDOW_DAYS);

        int score = 100;
        score -= (int) Math.min(48, (riskSnapshot.dueMileageCount() + riskSnapshot.dueTimeCount()) * 12);
        score -= (int) Math.min(20, (riskSnapshot.upcomingMileageCount() + riskSnapshot.upcomingTimeCount()) * 5);
        score -= (int) Math.min(20, unreadCount * 2);
        score = Math.max(0, score);

        int riskPoint = (int) ((riskSnapshot.dueMileageCount() + riskSnapshot.dueTimeCount()) * 3
            + (riskSnapshot.upcomingMileageCount() + riskSnapshot.upcomingTimeCount())
            + Math.min(3, unreadCount));
        String riskLevel = riskPoint >= 8 ? "HIGH" : riskPoint >= 4 ? "MEDIUM" : "LOW";

        return new MaintenanceAlertSummaryResponse(
            unreadCount,
            riskSnapshot.dueMileageCount(),
            riskSnapshot.dueTimeCount(),
            riskSnapshot.upcomingMileageCount(),
            riskSnapshot.upcomingTimeCount(),
            score,
            riskLevel);
    }

    @Transactional
    public void scanSingleUserVehicles(Long userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserId(userId);
        scanVehicles(vehicles, "single-user-scan");
    }

    private void scanVehicles(List<Vehicle> vehicles, String scanSource) {
        LocalDateTime now = LocalDateTime.now();
        long startNanos = System.nanoTime();
        int skipped = 0;
        int evaluated = 0;
        int created = 0;
        int errors = 0;

        for (Vehicle vehicle : vehicles) {
            if (vehicle.getUserId() == null || vehicle.getId() == null) {
                skipped++;
                continue;
            }

            try {
                EnumSet<AlertType> dueAlertTypes = maintenanceAlertRuleEvaluator.evaluateDueAlertTypes(
                        vehicle,
                        now,
                        MAINTENANCE_MILEAGE_THRESHOLD);

                evaluated++;
                if (dueAlertTypes.contains(AlertType.MILEAGE_OVERDUE)
                        && createAlertIfAbsent(vehicle, AlertType.MILEAGE_OVERDUE,
                                "车辆里程距离上次保养已超过10000公里，建议尽快预约保养。")) {
                    created++;
                }

                if (dueAlertTypes.contains(AlertType.TIME_OVERDUE)
                        && createAlertIfAbsent(vehicle, AlertType.TIME_OVERDUE,
                                "车辆距离上次保养已超过6个月，建议进行周期保养检查。")) {
                    created++;
                }
            } catch (Exception e) {
                errors++;
                logScanFailure(vehicle, scanSource, e);
            }
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            ScanMetrics metrics = new ScanMetrics(vehicles.size(), evaluated, skipped, created, errors, elapsedMs);
            logScanSummary(scanSource, metrics);
    }

            private void logScanFailure(Vehicle vehicle, String scanSource, Exception e) {
            logger.warn(
                "alertEvent={} source={} vehicleId={} userId={} message={}",
                ALERT_EVENT_SCAN_FAILURE,
                scanSource,
                vehicle.getId(),
                vehicle.getUserId(),
                e.getMessage(),
                e);
            }

            private void logScanSummary(String scanSource, ScanMetrics metrics) {
            String message = "alertEvent={} source={} totalVehicles={} evaluated={} skipped={} createdAlerts={} errors={} elapsedMs={}";
            if (metrics.errors() >= ERROR_WARN_THRESHOLD) {
                logger.warn(
                    message,
                    ALERT_EVENT_SCAN_SUMMARY,
                    scanSource,
                    metrics.totalVehicles(),
                    metrics.evaluated(),
                    metrics.skipped(),
                    metrics.createdAlerts(),
                    metrics.errors(),
                    metrics.elapsedMs());
                return;
            }

            logger.info(
                message,
                ALERT_EVENT_SCAN_SUMMARY,
                scanSource,
                metrics.totalVehicles(),
                metrics.evaluated(),
                metrics.skipped(),
                metrics.createdAlerts(),
                metrics.errors(),
                metrics.elapsedMs());
            }

            private record ScanMetrics(int totalVehicles,
                           int evaluated,
                           int skipped,
                           int createdAlerts,
                           int errors,
                           long elapsedMs) {
            }

    @Transactional
    public boolean markRead(Long alertId, Long userId) {
        return maintenanceAlertRepository.findByIdAndUserId(alertId, userId)
                .map(alert -> {
                    if (alert.getStatus() == AlertStatus.UNREAD) {
                        alert.setStatus(AlertStatus.READ);
                        maintenanceAlertRepository.save(alert);
                    }
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return maintenanceAlertRepository.updateStatusByUserId(
                userId, AlertStatus.UNREAD, AlertStatus.READ);
    }

    @Transactional
    public int markReadBatch(Long userId, List<Long> ids) {
        List<Long> safeIds = ids == null ? Collections.emptyList() : ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (safeIds.isEmpty()) {
            return 0;
        }
        return maintenanceAlertRepository.updateStatusByUserIdAndIds(
                userId, safeIds, AlertStatus.UNREAD, AlertStatus.READ);
    }

    private AlertStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        try {
            return AlertStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status 参数非法");
        }
    }

    private AlertType parseAlertType(String alertType) {
        if (alertType == null || alertType.isBlank() || "ALL".equalsIgnoreCase(alertType)) {
            return null;
        }
        String normalized = alertType.trim().toUpperCase();
        try {
            return AlertType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("alertType 参数非法");
        }
    }
}

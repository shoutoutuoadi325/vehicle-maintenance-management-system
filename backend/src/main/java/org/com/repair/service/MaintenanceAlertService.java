package org.com.repair.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
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
import org.com.repair.repository.MaintenanceAlertRepository;
import org.com.repair.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceAlertService {

    private static final int MAINTENANCE_MILEAGE_THRESHOLD = 10000;
    private static final int UPCOMING_MILEAGE_WINDOW = 1000;
    private static final int UPCOMING_TIME_WINDOW_DAYS = 15;
    private static final Set<AlertStatus> UNRESOLVED_STATUSES = Set.of(AlertStatus.UNREAD, AlertStatus.READ);

    private final VehicleRepository vehicleRepository;
    private final MaintenanceAlertRepository maintenanceAlertRepository;

    public MaintenanceAlertService(VehicleRepository vehicleRepository,
                                   MaintenanceAlertRepository maintenanceAlertRepository) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceAlertRepository = maintenanceAlertRepository;
    }

    @Transactional
    @Scheduled(cron = "${maintenance.alert.scan-cron:0 0 2 * * *}")
    public void scanVehiclesForMaintenance() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Vehicle vehicle : vehicles) {
            if (vehicle.getUserId() == null || vehicle.getId() == null) {
                continue;
            }

            Integer currentMileage = vehicle.getCurrentMileage();
            Integer lastMaintenanceMileage = vehicle.getLastMaintenanceMileage();
            boolean mileageOverdue = currentMileage != null
                    && lastMaintenanceMileage != null
                    && currentMileage - lastMaintenanceMileage >= MAINTENANCE_MILEAGE_THRESHOLD;

            LocalDateTime lastMaintenanceAt = vehicle.getLastMaintenanceAt() == null
                    ? null
                    : LocalDateTime.ofInstant(vehicle.getLastMaintenanceAt().toInstant(), ZoneId.systemDefault());
            boolean timeOverdue = lastMaintenanceAt != null && lastMaintenanceAt.isBefore(now.minusMonths(6));

            if (mileageOverdue) {
                createAlertIfAbsent(vehicle, AlertType.MILEAGE_OVERDUE,
                        "车辆里程距离上次保养已超过10000公里，建议尽快预约保养。");
            }

            if (timeOverdue) {
                createAlertIfAbsent(vehicle, AlertType.TIME_OVERDUE,
                        "车辆距离上次保养已超过6个月，建议进行周期保养检查。");
            }
        }
    }

    private void createAlertIfAbsent(Vehicle vehicle, AlertType alertType, String message) {
        boolean exists = maintenanceAlertRepository.existsByVehicleIdAndAlertTypeAndStatusIn(
                vehicle.getId(), alertType, UNRESOLVED_STATUSES);
        if (exists) {
            return;
        }

        MaintenanceAlert alert = new MaintenanceAlert();
        alert.setUserId(vehicle.getUserId());
        alert.setVehicleId(vehicle.getId());
        alert.setAlertType(alertType);
        alert.setMessage(message);
        alert.setTriggerTime(LocalDateTime.now());
        alert.setStatus(AlertStatus.UNREAD);
        maintenanceAlertRepository.save(alert);
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
        RiskSnapshot riskSnapshot = evaluateVehicleRisk(vehicles, LocalDateTime.now());

        int score = 100;
        score -= (int) Math.min(48, (riskSnapshot.dueMileageCount + riskSnapshot.dueTimeCount) * 12);
        score -= (int) Math.min(20, (riskSnapshot.upcomingMileageCount + riskSnapshot.upcomingTimeCount) * 5);
        score -= (int) Math.min(20, unreadCount * 2);
        score = Math.max(0, score);

        int riskPoint = (int) ((riskSnapshot.dueMileageCount + riskSnapshot.dueTimeCount) * 3
            + (riskSnapshot.upcomingMileageCount + riskSnapshot.upcomingTimeCount)
            + Math.min(3, unreadCount));
        String riskLevel = riskPoint >= 8 ? "HIGH" : riskPoint >= 4 ? "MEDIUM" : "LOW";

        return new MaintenanceAlertSummaryResponse(
            unreadCount,
            riskSnapshot.dueMileageCount,
            riskSnapshot.dueTimeCount,
            riskSnapshot.upcomingMileageCount,
            riskSnapshot.upcomingTimeCount,
            score,
            riskLevel);
    }

    @Transactional
    public void scanSingleUserVehicles(Long userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        for (Vehicle vehicle : vehicles) {
            Integer currentMileage = vehicle.getCurrentMileage();
            Integer lastMaintenanceMileage = vehicle.getLastMaintenanceMileage();
            boolean mileageOverdue = currentMileage != null
                    && lastMaintenanceMileage != null
                    && currentMileage - lastMaintenanceMileage >= MAINTENANCE_MILEAGE_THRESHOLD;

            LocalDateTime lastMaintenanceAt = vehicle.getLastMaintenanceAt() == null
                    ? null
                    : LocalDateTime.ofInstant(vehicle.getLastMaintenanceAt().toInstant(), ZoneId.systemDefault());
            boolean timeOverdue = lastMaintenanceAt != null && lastMaintenanceAt.isBefore(now.minusMonths(6));

            if (mileageOverdue) {
                createAlertIfAbsent(vehicle, AlertType.MILEAGE_OVERDUE,
                        "车辆里程距离上次保养已超过10000公里，建议尽快预约保养。");
            }

            if (timeOverdue) {
                createAlertIfAbsent(vehicle, AlertType.TIME_OVERDUE,
                        "车辆距离上次保养已超过6个月，建议进行周期保养检查。");
            }
        }
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

    private RiskSnapshot evaluateVehicleRisk(List<Vehicle> vehicles, LocalDateTime now) {
        long dueMileageCount = 0;
        long dueTimeCount = 0;
        long upcomingMileageCount = 0;
        long upcomingTimeCount = 0;

        LocalDateTime dueTimeThreshold = now.minusMonths(6);
        LocalDateTime upcomingTimeThreshold = dueTimeThreshold.plusDays(UPCOMING_TIME_WINDOW_DAYS);

        for (Vehicle vehicle : vehicles) {
            Integer currentMileage = vehicle.getCurrentMileage();
            Integer lastMaintenanceMileage = vehicle.getLastMaintenanceMileage();
            if (currentMileage != null && lastMaintenanceMileage != null) {
                int sinceLastMaintenance = currentMileage - lastMaintenanceMileage;
                if (sinceLastMaintenance >= MAINTENANCE_MILEAGE_THRESHOLD) {
                    dueMileageCount++;
                } else if (sinceLastMaintenance >= MAINTENANCE_MILEAGE_THRESHOLD - UPCOMING_MILEAGE_WINDOW) {
                    upcomingMileageCount++;
                }
            }

            if (vehicle.getLastMaintenanceAt() != null) {
                LocalDateTime lastMaintenanceAt = LocalDateTime.ofInstant(
                        vehicle.getLastMaintenanceAt().toInstant(), ZoneId.systemDefault());
                if (lastMaintenanceAt.isBefore(dueTimeThreshold)) {
                    dueTimeCount++;
                } else if (!lastMaintenanceAt.isAfter(upcomingTimeThreshold)) {
                    upcomingTimeCount++;
                }
            }
        }

        return new RiskSnapshot(dueMileageCount, dueTimeCount, upcomingMileageCount, upcomingTimeCount);
    }

    private record RiskSnapshot(long dueMileageCount,
                                long dueTimeCount,
                                long upcomingMileageCount,
                                long upcomingTimeCount) {
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

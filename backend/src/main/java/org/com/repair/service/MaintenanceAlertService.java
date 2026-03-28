package org.com.repair.service;

import org.com.repair.entity.MaintenanceAlert;
import org.com.repair.entity.MaintenanceAlert.AlertStatus;
import org.com.repair.entity.MaintenanceAlert.AlertType;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.MaintenanceAlertRepository;
import org.com.repair.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class MaintenanceAlertService {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceAlertService.class);

    // Trigger alert if mileage since last maintenance exceeds 10000 km
    private static final int MILEAGE_THRESHOLD = 10000;
    // Trigger alert if last maintenance was more than 6 months ago
    private static final int MONTHS_THRESHOLD = 6;

    private final VehicleRepository vehicleRepository;
    private final MaintenanceAlertRepository maintenanceAlertRepository;

    public MaintenanceAlertService(VehicleRepository vehicleRepository,
                                   MaintenanceAlertRepository maintenanceAlertRepository) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceAlertRepository = maintenanceAlertRepository;
    }

    /**
     * Daily scheduled task to check all vehicles and generate maintenance alerts.
     * Runs every day at 8:00 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkAllVehiclesForMaintenance() {
        logger.info("Starting scheduled maintenance alert check...");
        List<Vehicle> vehicles = vehicleRepository.findAll();
        int alertsCreated = 0;
        for (Vehicle vehicle : vehicles) {
            alertsCreated += checkVehicleAndCreateAlerts(vehicle);
        }
        logger.info("Maintenance alert check completed. Created {} new alerts.", alertsCreated);
    }

    /**
     * Check a single vehicle and create alerts if thresholds are exceeded.
     * Returns the number of new alerts created.
     */
    @Transactional
    public int checkVehicleAndCreateAlerts(Vehicle vehicle) {
        int created = 0;

        // Check mileage-based alert
        if (shouldTriggerMileageAlert(vehicle)) {
            boolean exists = maintenanceAlertRepository.existsByVehicleIdAndAlertTypeAndStatus(
                    vehicle.getId(), AlertType.MILEAGE_OVERDUE, AlertStatus.UNREAD);
            if (!exists) {
                MaintenanceAlert alert = new MaintenanceAlert();
                alert.setVehicle(vehicle);
                alert.setAlertType(AlertType.MILEAGE_OVERDUE);
                alert.setTriggeredAt(new Date());
                alert.setStatus(AlertStatus.UNREAD);
                alert.setMessage(String.format(
                        "车辆 %s %s（%s）当前里程 %d km，已超过上次保养里程 %d km，建议尽快进行保养。",
                        vehicle.getBrand(), vehicle.getModel(), vehicle.getLicensePlate(),
                        vehicle.getMileage(), MILEAGE_THRESHOLD));
                maintenanceAlertRepository.save(alert);
                created++;
            }
        }

        // Check time-based alert
        if (shouldTriggerTimeAlert(vehicle)) {
            boolean exists = maintenanceAlertRepository.existsByVehicleIdAndAlertTypeAndStatus(
                    vehicle.getId(), AlertType.TIME_OVERDUE, AlertStatus.UNREAD);
            if (!exists) {
                MaintenanceAlert alert = new MaintenanceAlert();
                alert.setVehicle(vehicle);
                alert.setAlertType(AlertType.TIME_OVERDUE);
                alert.setTriggeredAt(new Date());
                alert.setStatus(AlertStatus.UNREAD);
                alert.setMessage(String.format(
                        "车辆 %s %s（%s）距上次保养已超过 %d 个月，建议尽快进行保养。",
                        vehicle.getBrand(), vehicle.getModel(), vehicle.getLicensePlate(),
                        MONTHS_THRESHOLD));
                maintenanceAlertRepository.save(alert);
                created++;
            }
        }

        return created;
    }

    private boolean shouldTriggerMileageAlert(Vehicle vehicle) {
        if (vehicle.getMileage() == null) {
            return false;
        }
        return vehicle.getMileage() >= MILEAGE_THRESHOLD;
    }

    private boolean shouldTriggerTimeAlert(Vehicle vehicle) {
        if (vehicle.getLastMaintenanceDate() == null) {
            return false;
        }
        Calendar threshold = Calendar.getInstance();
        threshold.add(Calendar.MONTH, -MONTHS_THRESHOLD);
        return vehicle.getLastMaintenanceDate().before(threshold.getTime());
    }

    public List<MaintenanceAlert> getAlertsByVehicleId(Long vehicleId) {
        return maintenanceAlertRepository.findByVehicleId(vehicleId);
    }

    public List<MaintenanceAlert> getUnreadAlertsByVehicleId(Long vehicleId) {
        return maintenanceAlertRepository.findByVehicleIdAndStatus(vehicleId, AlertStatus.UNREAD);
    }

    @Transactional
    public void markAlertAsRead(Long alertId) {
        maintenanceAlertRepository.findById(alertId).ifPresent(alert -> {
            alert.setStatus(AlertStatus.READ);
            maintenanceAlertRepository.save(alert);
        });
    }

    @Transactional
    public void markAllVehicleAlertsAsRead(Long vehicleId) {
        List<MaintenanceAlert> alerts = maintenanceAlertRepository
                .findByVehicleIdAndStatus(vehicleId, AlertStatus.UNREAD);
        alerts.forEach(a -> a.setStatus(AlertStatus.READ));
        maintenanceAlertRepository.saveAll(alerts);
    }
}

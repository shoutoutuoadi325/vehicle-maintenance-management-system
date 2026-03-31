package org.com.repair.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import org.com.repair.entity.MaintenanceAlert.AlertType;
import org.com.repair.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceAlertRuleEvaluator {

    public EnumSet<AlertType> evaluateDueAlertTypes(Vehicle vehicle,
                                                     LocalDateTime now,
                                                     int maintenanceMileageThreshold) {
        EnumSet<AlertType> dueAlertTypes = EnumSet.noneOf(AlertType.class);

        Integer currentMileage = vehicle.getCurrentMileage();
        Integer lastMaintenanceMileage = vehicle.getLastMaintenanceMileage();
        boolean mileageOverdue = currentMileage != null
                && lastMaintenanceMileage != null
                && currentMileage - lastMaintenanceMileage >= maintenanceMileageThreshold;

        LocalDateTime lastMaintenanceAt = vehicle.getLastMaintenanceAt() == null
                ? null
                : LocalDateTime.ofInstant(vehicle.getLastMaintenanceAt().toInstant(), ZoneId.systemDefault());
        boolean timeOverdue = lastMaintenanceAt != null && lastMaintenanceAt.isBefore(now.minusMonths(6));

        if (mileageOverdue) {
            dueAlertTypes.add(AlertType.MILEAGE_OVERDUE);
        }

        if (timeOverdue) {
            dueAlertTypes.add(AlertType.TIME_OVERDUE);
        }

        return dueAlertTypes;
    }

    public VehicleRiskSnapshot evaluateVehicleRisk(List<Vehicle> vehicles,
                                                   LocalDateTime now,
                                                   int maintenanceMileageThreshold,
                                                   int upcomingMileageWindow,
                                                   int upcomingTimeWindowDays) {
        long dueMileageCount = 0;
        long dueTimeCount = 0;
        long upcomingMileageCount = 0;
        long upcomingTimeCount = 0;

        LocalDateTime dueTimeThreshold = now.minusMonths(6);
        LocalDateTime upcomingTimeThreshold = dueTimeThreshold.plusDays(upcomingTimeWindowDays);

        for (Vehicle vehicle : vehicles) {
            Integer currentMileage = vehicle.getCurrentMileage();
            Integer lastMaintenanceMileage = vehicle.getLastMaintenanceMileage();
            if (currentMileage != null && lastMaintenanceMileage != null) {
                int sinceLastMaintenance = currentMileage - lastMaintenanceMileage;
                if (sinceLastMaintenance >= maintenanceMileageThreshold) {
                    dueMileageCount++;
                } else if (sinceLastMaintenance >= maintenanceMileageThreshold - upcomingMileageWindow) {
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

        return new VehicleRiskSnapshot(dueMileageCount, dueTimeCount, upcomingMileageCount, upcomingTimeCount);
    }

    public record VehicleRiskSnapshot(long dueMileageCount,
                                      long dueTimeCount,
                                      long upcomingMileageCount,
                                      long upcomingTimeCount) {
    }
}

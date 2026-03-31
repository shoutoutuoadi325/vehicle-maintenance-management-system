package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.com.repair.entity.MaintenanceAlert.AlertType;
import org.com.repair.entity.Vehicle;
import org.junit.jupiter.api.Test;

class MaintenanceAlertRuleEvaluatorTest {

    private final MaintenanceAlertRuleEvaluator evaluator = new MaintenanceAlertRuleEvaluator();

    @Test
    void shouldIdentifyMileageAndTimeOverdueAlertTypes() {
        LocalDateTime now = LocalDateTime.now();
        Vehicle vehicle = vehicle(18000, 6000, now.minusMonths(8));

        var alertTypes = evaluator.evaluateDueAlertTypes(vehicle, now, 10000);

        assertTrue(alertTypes.contains(AlertType.MILEAGE_OVERDUE));
        assertTrue(alertTypes.contains(AlertType.TIME_OVERDUE));
    }

    @Test
    void shouldCalculateRiskSnapshotWithDueAndUpcomingBuckets() {
        LocalDateTime now = LocalDateTime.now();
        Vehicle due = vehicle(22000, 10000, now.minusMonths(8));
        Vehicle upcoming = vehicle(19500, 10000, now.minusMonths(6).plusDays(10));

        MaintenanceAlertRuleEvaluator.VehicleRiskSnapshot snapshot = evaluator.evaluateVehicleRisk(
                List.of(due, upcoming),
                now,
                10000,
                1000,
                15);

        assertEquals(1, snapshot.dueMileageCount());
        assertEquals(1, snapshot.dueTimeCount());
        assertEquals(1, snapshot.upcomingMileageCount());
        assertEquals(1, snapshot.upcomingTimeCount());
    }

    private Vehicle vehicle(Integer currentMileage, Integer lastMaintenanceMileage, LocalDateTime lastMaintenanceAt) {
        Vehicle vehicle = new Vehicle();
        vehicle.setCurrentMileage(currentMileage);
        vehicle.setLastMaintenanceMileage(lastMaintenanceMileage);
        vehicle.setLastMaintenanceAt(Date.from(lastMaintenanceAt.atZone(ZoneId.systemDefault()).toInstant()));
        return vehicle;
    }
}

package org.com.repair.DTO;

import java.time.LocalDateTime;

import org.com.repair.entity.MaintenanceAlert;

public record MaintenanceAlertResponse(
        Long id,
        Long userId,
        Long vehicleId,
        String alertType,
        String message,
        LocalDateTime triggerTime,
        String status
) {
    public MaintenanceAlertResponse(MaintenanceAlert alert) {
        this(
                alert.getId(),
                alert.getUserId(),
                alert.getVehicleId(),
                alert.getAlertType().name(),
                alert.getMessage(),
                alert.getTriggerTime(),
                alert.getStatus().name()
        );
    }
}

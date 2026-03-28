package org.com.repair.DTO;

import org.com.repair.entity.MaintenanceAlert;

import java.util.Date;

public record MaintenanceAlertResponse(
        Long id,
        Long vehicleId,
        String vehicleLicensePlate,
        String vehicleBrand,
        String vehicleModel,
        String alertType,
        Date triggeredAt,
        String status,
        String message
) {
    public MaintenanceAlertResponse(MaintenanceAlert alert) {
        this(
                alert.getId(),
                alert.getVehicleId(),
                alert.getVehicle() != null ? alert.getVehicle().getLicensePlate() : null,
                alert.getVehicle() != null ? alert.getVehicle().getBrand() : null,
                alert.getVehicle() != null ? alert.getVehicle().getModel() : null,
                alert.getAlertType() != null ? alert.getAlertType().name() : null,
                alert.getTriggeredAt(),
                alert.getStatus() != null ? alert.getStatus().name() : null,
                alert.getMessage()
        );
    }
}

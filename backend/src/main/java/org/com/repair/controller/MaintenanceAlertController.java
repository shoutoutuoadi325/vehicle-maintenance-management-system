package org.com.repair.controller;

import org.com.repair.DTO.MaintenanceAlertResponse;
import org.com.repair.service.MaintenanceAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maintenance-alerts")
public class MaintenanceAlertController {

    private final MaintenanceAlertService maintenanceAlertService;

    public MaintenanceAlertController(MaintenanceAlertService maintenanceAlertService) {
        this.maintenanceAlertService = maintenanceAlertService;
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceAlertResponse>> getAlertsByVehicle(@PathVariable Long vehicleId) {
        List<MaintenanceAlertResponse> alerts = maintenanceAlertService.getAlertsByVehicleId(vehicleId)
                .stream()
                .map(MaintenanceAlertResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/vehicle/{vehicleId}/unread")
    public ResponseEntity<List<MaintenanceAlertResponse>> getUnreadAlertsByVehicle(@PathVariable Long vehicleId) {
        List<MaintenanceAlertResponse> alerts = maintenanceAlertService.getUnreadAlertsByVehicleId(vehicleId)
                .stream()
                .map(MaintenanceAlertResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/{alertId}/read")
    public ResponseEntity<Void> markAlertAsRead(@PathVariable Long alertId) {
        maintenanceAlertService.markAlertAsRead(alertId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/vehicle/{vehicleId}/read-all")
    public ResponseEntity<Void> markAllVehicleAlertsAsRead(@PathVariable Long vehicleId) {
        maintenanceAlertService.markAllVehicleAlertsAsRead(vehicleId);
        return ResponseEntity.noContent().build();
    }
}

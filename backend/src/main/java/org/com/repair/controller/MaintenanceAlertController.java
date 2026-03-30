package org.com.repair.controller;

import java.util.List;
import java.util.Map;

import org.com.repair.DTO.ApiResponse;
import org.com.repair.DTO.MaintenanceAlertPageResponse;
import org.com.repair.DTO.MaintenanceAlertResponse;
import org.com.repair.DTO.MaintenanceAlertSummaryResponse;
import org.com.repair.entity.MaintenanceAlert;
import org.com.repair.security.RequestUserContextResolver;
import org.com.repair.service.MaintenanceAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/maintenance-alerts")
public class MaintenanceAlertController {

    private final MaintenanceAlertService maintenanceAlertService;
    private final RequestUserContextResolver requestUserContextResolver;

    public MaintenanceAlertController(MaintenanceAlertService maintenanceAlertService,
                                      RequestUserContextResolver requestUserContextResolver) {
        this.maintenanceAlertService = maintenanceAlertService;
        this.requestUserContextResolver = requestUserContextResolver;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<MaintenanceAlertResponse>>> getUserAlerts(@PathVariable Long userId,
                                                                                      HttpServletRequest request) {
        requestUserContextResolver.ensurePathUserMatch(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(maintenanceAlertService.getUserAlerts(userId)));
    }

    @GetMapping("/user/{userId}/page")
    public ResponseEntity<ApiResponse<MaintenanceAlertPageResponse>> getUserAlertsPage(@PathVariable Long userId,
                                                                                         @RequestParam(defaultValue = "0") int page,
                                                                                         @RequestParam(defaultValue = "10") int size,
                                                                                         @RequestParam(required = false) String status,
                                                                                         @RequestParam(required = false) String alertType,
                                                                                         HttpServletRequest request) {
        requestUserContextResolver.ensurePathUserMatch(request, userId);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        return ResponseEntity.ok(ApiResponse.ok(
            maintenanceAlertService.getUserAlertsPage(
                userId,
                safePage,
                safeSize,
                normalizeStatus(status),
                normalizeAlertType(alertType))));
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<ApiResponse<MaintenanceAlertSummaryResponse>> getUserSummary(@PathVariable Long userId,
                                                                                        HttpServletRequest request) {
        requestUserContextResolver.ensurePathUserMatch(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(maintenanceAlertService.getUserSummary(userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requestUserContextResolver.requireUserId(request);
        boolean updated = maintenanceAlertService.markRead(id, userId);
        if (!updated) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("ALERT_NOT_FOUND", "提醒不存在"));
        }
        return ResponseEntity.ok(ApiResponse.ok("提醒已标记为已读", Map.of("updated", 1)));
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead(@PathVariable Long userId,
                                                                         HttpServletRequest request) {
        requestUserContextResolver.ensurePathUserMatch(request, userId);
        int updated = maintenanceAlertService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("批量标记已读完成", Map.of("updated", updated)));
    }

    @PutMapping("/user/{userId}/read-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markBatchRead(@PathVariable Long userId,
                                                                           @RequestBody ReadBatchRequest readBatchRequest,
                                                                           HttpServletRequest request) {
        requestUserContextResolver.ensurePathUserMatch(request, userId);
        int updated = maintenanceAlertService.markReadBatch(
                userId,
                readBatchRequest == null ? List.of() : readBatchRequest.ids());
        return ResponseEntity.ok(ApiResponse.ok("批量标记已读完成", Map.of("updated", updated)));
    }

    public record ReadBatchRequest(List<Long> ids) {
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        try {
            MaintenanceAlert.AlertStatus.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status 参数非法");
        }
    }

    private String normalizeAlertType(String alertType) {
        if (alertType == null || alertType.isBlank() || "ALL".equalsIgnoreCase(alertType)) {
            return null;
        }
        String normalized = alertType.trim().toUpperCase();
        try {
            MaintenanceAlert.AlertType.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("alertType 参数非法");
        }
    }
}

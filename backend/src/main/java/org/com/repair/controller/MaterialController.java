package org.com.repair.controller;

import java.util.List;
import java.util.Map;

import org.com.repair.DTO.ApiResponse;
import org.com.repair.DTO.InventoryAlertPageResponse;
import org.com.repair.entity.InventoryAlertNotification;
import org.com.repair.DTO.MaterialResponse;
import org.com.repair.DTO.NewMaterialRequest;
import org.com.repair.security.RequestUserContextResolver;
import org.com.repair.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    
    private final MaterialService materialService;
    private final RequestUserContextResolver requestUserContextResolver;
    
    public MaterialController(MaterialService materialService,
                              RequestUserContextResolver requestUserContextResolver) {
        this.materialService = materialService;
        this.requestUserContextResolver = requestUserContextResolver;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<MaterialResponse>> addMaterial(@RequestBody NewMaterialRequest request) {
        MaterialResponse response = materialService.addMaterial(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("材料创建成功", response));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialResponse>> getMaterialById(@PathVariable Long id) {
        return materialService.getMaterialById(id)
                .map(material -> ResponseEntity.ok(ApiResponse.ok(material)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("MATERIAL_NOT_FOUND", "材料不存在")));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getAllMaterials() {
        List<MaterialResponse> materials = materialService.getAllMaterials();
        return ResponseEntity.ok(ApiResponse.ok(materials));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getMaterialsByName(@RequestParam String name) {
        List<MaterialResponse> materials = materialService.getMaterialsByName(name);
        return ResponseEntity.ok(ApiResponse.ok(materials));
    }
    
    @GetMapping("/price-range")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getMaterialsByPriceRange(
            @RequestParam Double minPrice, 
            @RequestParam Double maxPrice) {
        List<MaterialResponse> materials = materialService.getMaterialsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(ApiResponse.ok(materials));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialResponse>> updateMaterial(
            @PathVariable Long id, 
            @RequestBody NewMaterialRequest request) {
        try {
            MaterialResponse response = materialService.updateMaterial(id, request);
            return ResponseEntity.ok(ApiResponse.ok("材料更新成功", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MATERIAL_NOT_FOUND", "材料不存在"));
        }
    }

    @PutMapping("/{id}/consume")
    public ResponseEntity<ApiResponse<MaterialResponse>> consumeMaterialStock(
            @PathVariable Long id,
            @RequestBody ConsumeMaterialRequest request) {
        try {
            if (request == null || request.quantity() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("BAD_REQUEST", "quantity 不能为空"));
            }
            MaterialResponse response = materialService.consumeMaterialStock(id, request.quantity());
            return ResponseEntity.ok(ApiResponse.ok("材料库存扣减成功", response));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MATERIAL_NOT_FOUND", ex.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteMaterial(@PathVariable Long id) {
        boolean deleted = materialService.deleteMaterial(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MATERIAL_NOT_FOUND", "材料不存在"));
        }
        return ResponseEntity.ok(ApiResponse.ok("材料删除成功", Map.of("deleted", true)));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<InventoryAlertNotification>>> getInventoryAlerts(HttpServletRequest request) {
        requestUserContextResolver.requireAdminRole(request);
        return ResponseEntity.ok(ApiResponse.ok(materialService.getActiveInventoryAlerts()));
    }

    @GetMapping("/notifications/page")
    public ResponseEntity<ApiResponse<InventoryAlertPageResponse>> getInventoryAlertsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String severity,
            HttpServletRequest request) {
        requestUserContextResolver.requireAdminRole(request);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        String normalizedSeverity = normalizeSeverity(severity);
        return ResponseEntity.ok(ApiResponse.ok(
            materialService.getActiveInventoryAlertsPage(safePage, safeSize, normalizedSeverity)));
    }

    @PutMapping("/notifications/{id}/resolve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveInventoryAlert(@PathVariable Long id,
                                                                                   HttpServletRequest request) {
        requestUserContextResolver.requireAdminRole(request);
        boolean resolved = materialService.resolveInventoryAlert(id);
        if (!resolved) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("INVENTORY_ALERT_NOT_FOUND", "预警不存在或已处理"));
        }
        return ResponseEntity.ok(ApiResponse.ok("预警已处理", Map.of("updated", 1)));
    }

    @PutMapping("/notifications/resolve-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveInventoryAlerts(
            @RequestBody ResolveBatchRequest resolveBatchRequest,
            HttpServletRequest request) {
        requestUserContextResolver.requireAdminRole(request);
        int updated = materialService.resolveInventoryAlerts(
                resolveBatchRequest == null ? List.of() : resolveBatchRequest.ids());
        return ResponseEntity.ok(ApiResponse.ok("批量处理库存预警完成", Map.of("updated", updated)));
    }

    public record ConsumeMaterialRequest(Integer quantity) {
    }

    public record ResolveBatchRequest(List<Long> ids) {
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank() || "ALL".equalsIgnoreCase(severity)) {
            return null;
        }
        String normalized = severity.trim().toUpperCase();
        if ("CRITICAL".equals(normalized) || "WARNING".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("severity 参数非法");
    }
} 
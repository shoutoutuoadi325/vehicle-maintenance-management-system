package org.com.repair.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.com.repair.DTO.InventoryAlertPageResponse;
import org.com.repair.DTO.MaterialResponse;
import org.com.repair.DTO.NewMaterialRequest;
import org.com.repair.entity.InventoryAlertNotification;
import org.com.repair.entity.InventoryAlertNotification.AlertStatus;
import org.com.repair.entity.Material;
import org.com.repair.repository.InventoryAlertNotificationRepository;
import org.com.repair.repository.MaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {
    
    private final MaterialRepository materialRepository;
    private final InventoryAlertNotificationRepository inventoryAlertNotificationRepository;
    
    public MaterialService(MaterialRepository materialRepository,
                           InventoryAlertNotificationRepository inventoryAlertNotificationRepository) {
        this.materialRepository = materialRepository;
        this.inventoryAlertNotificationRepository = inventoryAlertNotificationRepository;
    }
    
    @Transactional
    public MaterialResponse addMaterial(NewMaterialRequest request) {
        Material material = new Material();
        material.setName(request.name());
        material.setUnitPrice(request.unitPrice());
        material.setStockQuantity(request.stockQuantity() == null ? 0 : Math.max(0, request.stockQuantity()));
        material.setMinimumStockLevel(request.minimumStockLevel() == null ? 10 : Math.max(0, request.minimumStockLevel()));
        Material saved = materialRepository.save(material);
        return new MaterialResponse(saved);
    }
    
    public Optional<MaterialResponse> getMaterialById(Long id) {
        return materialRepository.findById(Objects.requireNonNull(id, "材料ID不能为空"))
                .map(MaterialResponse::new);
    }
    
    @Transactional
    public MaterialResponse updateMaterial(Long id, NewMaterialRequest request) {
        Material material = materialRepository.findById(Objects.requireNonNull(id, "材料ID不能为空"))
                .orElseThrow(() -> new RuntimeException("材料不存在"));
        
        material.setName(request.name());
        material.setUnitPrice(request.unitPrice());
        if (request.stockQuantity() != null) {
            material.setStockQuantity(Math.max(0, request.stockQuantity()));
        }
        if (request.minimumStockLevel() != null) {
            material.setMinimumStockLevel(Math.max(0, request.minimumStockLevel()));
        }
        Material updated = materialRepository.save(material);
        return new MaterialResponse(updated);
    }
    
    @Transactional
    public boolean deleteMaterial(Long id) {
        Long materialId = Objects.requireNonNull(id, "材料ID不能为空");
        if (materialRepository.existsById(materialId)) {
            materialRepository.deleteById(materialId);
            return true;
        }
        return false;
    }
    
    public List<MaterialResponse> getAllMaterials() {
        return materialRepository.findAll().stream()
                .map(MaterialResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<MaterialResponse> getMaterialsByName(String name) {
        return materialRepository.findByNameContaining(name).stream()
                .map(MaterialResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<MaterialResponse> getMaterialsByPriceRange(Double minPrice, Double maxPrice) {
        return materialRepository.findByUnitPriceBetween(minPrice, maxPrice).stream()
                .map(MaterialResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaterialResponse consumeMaterialStock(Long materialId, int consumeQuantity) {
        if (consumeQuantity <= 0) {
            throw new IllegalArgumentException("消耗数量必须大于0");
        }

        Material material = materialRepository.findById(Objects.requireNonNull(materialId, "材料ID不能为空"))
                .orElseThrow(() -> new RuntimeException("材料不存在"));

        int currentStock = material.getStockQuantity() == null ? 0 : material.getStockQuantity();
        if (currentStock < consumeQuantity) {
            throw new IllegalStateException("库存不足，当前库存: " + currentStock);
        }

        int newStock = currentStock - consumeQuantity;
        material.setStockQuantity(newStock);
        Material saved = materialRepository.save(material);

        int minimum = saved.getMinimumStockLevel() == null ? 0 : saved.getMinimumStockLevel();
        if (newStock < minimum
                && !inventoryAlertNotificationRepository.existsByMaterialIdAndStatus(saved.getId(), AlertStatus.ACTIVE)) {
            InventoryAlertNotification notification = new InventoryAlertNotification();
            notification.setMaterialId(saved.getId());
            notification.setMaterialName(saved.getName());
            notification.setCurrentStock(newStock);
            notification.setMinimumStockLevel(minimum);
            notification.setStatus(AlertStatus.ACTIVE);
            inventoryAlertNotificationRepository.save(notification);
        }

        return new MaterialResponse(saved);
    }

    public List<InventoryAlertNotification> getActiveInventoryAlerts() {
        return inventoryAlertNotificationRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.ACTIVE);
    }

    public InventoryAlertPageResponse getActiveInventoryAlertsPage(int page, int size, String severity) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        String severityFilter = normalizeSeverity(severity);

        Page<InventoryAlertNotification> result = inventoryAlertNotificationRepository
                .searchByStatusAndSeverity(AlertStatus.ACTIVE, severityFilter, pageRequest);

        long criticalCount = inventoryAlertNotificationRepository.countCriticalByStatus(AlertStatus.ACTIVE);
        long warningCount = inventoryAlertNotificationRepository.countWarningByStatus(AlertStatus.ACTIVE);

        return new InventoryAlertPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                criticalCount,
                warningCount,
                result.getContent());
    }

    @Transactional
    public boolean resolveInventoryAlert(Long alertId) {
        return inventoryAlertNotificationRepository.findByIdAndStatus(alertId, AlertStatus.ACTIVE)
                .map(alert -> {
                    alert.setStatus(AlertStatus.RESOLVED);
                    alert.setResolvedAt(java.time.LocalDateTime.now());
                    inventoryAlertNotificationRepository.save(alert);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public int resolveInventoryAlerts(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (safeIds.isEmpty()) {
            return 0;
        }
        return inventoryAlertNotificationRepository.updateStatusByIds(
                safeIds,
                AlertStatus.ACTIVE,
                AlertStatus.RESOLVED);
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
package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Objects;
import java.util.Optional;

import org.com.repair.DTO.MaterialResponse;
import org.com.repair.entity.InventoryAlertNotification.AlertStatus;
import org.com.repair.entity.Material;
import org.com.repair.repository.InventoryAlertNotificationRepository;
import org.com.repair.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("null")
class MaterialServiceLowStockAlertTest {

    private MaterialRepository materialRepository;
    private InventoryAlertNotificationRepository inventoryAlertNotificationRepository;
    private MaterialService materialService;

    @BeforeEach
    void setUp() {
        materialRepository = mock(MaterialRepository.class);
        inventoryAlertNotificationRepository = mock(InventoryAlertNotificationRepository.class);
        materialService = new MaterialService(materialRepository, inventoryAlertNotificationRepository);
    }

    @Test
    void shouldCreateInventoryAlertAfterConsumeWhenBelowThreshold() {
        Material material = buildMaterial(5L, "机油", 10, 8);
        when(materialRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class)))
            .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0, Material.class)));
        when(inventoryAlertNotificationRepository.existsByMaterialIdAndStatus(5L, AlertStatus.ACTIVE)).thenReturn(false);

        MaterialResponse response = materialService.consumeMaterialStock(5L, 3);

        assertEquals(7, response.stockQuantity());
        verify(inventoryAlertNotificationRepository).save(org.mockito.ArgumentMatchers.isA(org.com.repair.entity.InventoryAlertNotification.class));
    }

    @Test
    void shouldNotCreateDuplicateActiveInventoryAlert() {
        Material material = buildMaterial(6L, "滤芯", 10, 9);
        when(materialRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class)))
            .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0, Material.class)));
        when(inventoryAlertNotificationRepository.existsByMaterialIdAndStatus(6L, AlertStatus.ACTIVE)).thenReturn(true);

        MaterialResponse response = materialService.consumeMaterialStock(6L, 2);

        assertEquals(8, response.stockQuantity());
        verify(inventoryAlertNotificationRepository, never())
            .save(org.mockito.ArgumentMatchers.isA(org.com.repair.entity.InventoryAlertNotification.class));
    }

    private Material buildMaterial(Long id, String name, int stock, int minimum) {
        Material material = new Material();
        material.setId(id);
        material.setName(name);
        material.setUnitPrice(100.0);
        material.setStockQuantity(stock);
        material.setMinimumStockLevel(minimum);
        return material;
    }
}

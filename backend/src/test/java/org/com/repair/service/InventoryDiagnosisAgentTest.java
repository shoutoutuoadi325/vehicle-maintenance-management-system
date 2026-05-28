package org.com.repair.service;

import org.com.repair.DTO.MaterialResponse;
import org.com.repair.entity.InventoryAlertNotification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryDiagnosisAgentTest {

    @Test
    void shouldReturnLowStockSummaryForRelatedMaterialAlert() {
        MaterialService materialService = mock(MaterialService.class);
        InventoryDiagnosisAgent agent = new InventoryDiagnosisAgent(materialService);
        InventoryAlertNotification alert = lowStockAlert(1L, "\u5239\u8f66\u7247", 2, 5);

        when(materialService.getAllMaterials()).thenReturn(List.of(
                new MaterialResponse(1L, "\u5239\u8f66\u7247", 90.0, 2, 5),
                new MaterialResponse(2L, "\u51b7\u5374\u6db2", 50.0, 20, 10)));
        when(materialService.getActiveInventoryAlerts()).thenReturn(List.of(alert));

        InventoryDiagnosisAgent.InventoryEvidence evidence = agent.analyze(
                "\u5239\u8f66\u5f02\u54cd\uff0c\u6000\u7591\u5239\u8f66\u7247\u78e8\u635f");

        assertTrue(evidence.hasLowStockRisk());
        assertTrue(evidence.summary().contains("Inventory Agent"));
        assertTrue(evidence.summary().contains("\u5239\u8f66\u7247"));
        assertTrue(evidence.summary().contains("INVENTORY_BELOW_SAFETY_STOCK"));
        assertTrue(evidence.summary().contains("INVENTORY_LOW_STOCK"));
        assertTrue(evidence.lowStockWarnings().contains("\u5239\u8f66\u7247\u5f53\u524d\u5e93\u5b58\u4f4e\u4e8e\u5b89\u5168\u5e93\u5b58\uff1a\u5f53\u524d 2\uff0c\u5b89\u5168\u5e93\u5b58 5\uff0c\u8bf7\u5148\u786e\u8ba4\u5907\u4ef6\u3002"));
        verify(materialService).getAllMaterials();
        verify(materialService).getActiveInventoryAlerts();
        verify(materialService, never()).consumeMaterialStock(anyLong(), anyInt());
        verify(materialService, never()).resolveInventoryAlert(anyLong());
        verify(materialService, never()).resolveInventoryAlerts(anyList());
    }

    @Test
    void shouldReturnLookupFailureEvidenceWhenInventoryReadFails() {
        MaterialService materialService = mock(MaterialService.class);
        InventoryDiagnosisAgent agent = new InventoryDiagnosisAgent(materialService);
        when(materialService.getAllMaterials()).thenThrow(new IllegalStateException("db unavailable"));

        InventoryDiagnosisAgent.InventoryEvidence evidence = agent.analyze("\u5239\u8f66\u5f02\u54cd");

        assertTrue(evidence.summary().contains("INVENTORY_LOOKUP_FAILED"));
    }

    @Test
    void shouldNotWarnWhenProjectedStockStaysAboveSafetyLevel() {
        MaterialService materialService = mock(MaterialService.class);
        InventoryDiagnosisAgent agent = new InventoryDiagnosisAgent(materialService);

        when(materialService.getAllMaterials()).thenReturn(List.of(
                new MaterialResponse(1L, "\u5239\u8f66\u7247", 90.0, 8, 5)));
        when(materialService.getActiveInventoryAlerts()).thenReturn(List.of());

        InventoryDiagnosisAgent.InventoryEvidence evidence = agent.analyze("\u5239\u8f66\u7247\u9700\u8981\u68c0\u67e5");

        assertTrue(evidence.lowStockWarnings().isEmpty());
    }

    private InventoryAlertNotification lowStockAlert(Long materialId, String materialName, int stock, int minimum) {
        InventoryAlertNotification alert = new InventoryAlertNotification();
        alert.setMaterialId(materialId);
        alert.setMaterialName(materialName);
        alert.setCurrentStock(stock);
        alert.setMinimumStockLevel(minimum);
        return alert;
    }
}

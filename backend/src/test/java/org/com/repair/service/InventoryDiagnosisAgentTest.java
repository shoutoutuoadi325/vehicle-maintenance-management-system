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
        assertTrue(evidence.summary().contains("INVENTORY_LOW_STOCK"));
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

    private InventoryAlertNotification lowStockAlert(Long materialId, String materialName, int stock, int minimum) {
        InventoryAlertNotification alert = new InventoryAlertNotification();
        alert.setMaterialId(materialId);
        alert.setMaterialName(materialName);
        alert.setCurrentStock(stock);
        alert.setMinimumStockLevel(minimum);
        return alert;
    }
}

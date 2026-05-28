package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.DTO.MaterialResponse;
import org.com.repair.entity.InventoryAlertNotification;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIDiagnosisServiceInventoryAgentTest {

    @Test
    void technicianSemanticPromptShouldIncludeInventoryEvidence() {
        MaterialService materialService = materialServiceWithPaintLowStock();
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService(materialService);

        AIDiagnosisResponse response = service.diagnoseFault(
                "\u6cb9\u6f06\u78e8\u635f\u600e\u4e48\u529e",
                "technician",
                7L);

        assertTrue(service.capturedPrompt.contains("Inventory Agent context"));
        assertTrue(service.capturedPrompt.contains("\u6cb9\u6f06"));
        assertTrue(service.capturedPrompt.contains("INVENTORY_LOW_STOCK"));
        assertTrue(response.getInventoryWarnings().contains("\u6cb9\u6f06\u5f53\u524d\u5e93\u5b58\u4f4e\u4e8e\u5b89\u5168\u5e93\u5b58\uff1a\u5f53\u524d 1\uff0c\u5b89\u5168\u5e93\u5b58 3\uff0c\u8bf7\u5148\u786e\u8ba4\u5907\u4ef6\u3002"));
        assertTrue(response.getDecisionPath().stream()
                .anyMatch(step -> step.contains("Inventory Agent") && step.contains("INVENTORY_LOW_STOCK")));
        verify(materialService).getAllMaterials();
        verify(materialService).getActiveInventoryAlerts();
        verify(materialService, never()).consumeMaterialStock(anyLong(), anyInt());
        verify(materialService, never()).resolveInventoryAlert(anyLong());
        verify(materialService, never()).resolveInventoryAlerts(anyList());
    }

    @Test
    void customerDiagnosisShouldNotCallInventoryAgent() {
        MaterialService materialService = mock(MaterialService.class);
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService(materialService);

        AIDiagnosisResponse response = service.diagnoseFault(
                "\u6cb9\u6f06\u78e8\u635f\u600e\u4e48\u529e",
                "customer",
                null);

        assertFalse(service.capturedPrompt.contains("Inventory Agent context"));
        assertTrue(response.getDecisionPath().isEmpty());
        assertTrue(response.getInventoryWarnings().isEmpty());
        verify(materialService, never()).getAllMaterials();
        verify(materialService, never()).getActiveInventoryAlerts();
    }

    private MaterialService materialServiceWithPaintLowStock() {
        MaterialService materialService = mock(MaterialService.class);
        InventoryAlertNotification alert = new InventoryAlertNotification();
        alert.setMaterialId(9L);
        alert.setMaterialName("\u6cb9\u6f06");
        alert.setCurrentStock(1);
        alert.setMinimumStockLevel(3);

        when(materialService.getAllMaterials()).thenReturn(List.of(
                new MaterialResponse(9L, "\u6cb9\u6f06", 120.0, 1, 3),
                new MaterialResponse(10L, "\u5239\u8f66\u7247", 90.0, 12, 5)));
        when(materialService.getActiveInventoryAlerts()).thenReturn(List.of(alert));
        return materialService;
    }

    private static class CapturingAIDiagnosisService extends AIDiagnosisService {
        private String capturedPrompt = "";

        private CapturingAIDiagnosisService(MaterialService materialService) {
            super(
                    mock(GamificationService.class),
                    mock(TechnicianService.class),
                    new RuleDiagnosisService(),
                    new PrivacyMaskingService(),
                    new SemanticDiagnosisAgent(),
                    new InventoryDiagnosisAgent(materialService),
                    mock(HistoryCaseAgent.class),
                    new DecisionFusionEngine(new SafetyNetGate()));
        }

        @Override
        protected String callOpenAIAPI(String prompt, String traceId, String stage) throws IOException {
            this.capturedPrompt = prompt;
            return """
                    {
                      "faultType": "paint wear",
                      "suggestion": "clean the paint surface, inspect primer exposure, then choose polishing or repainting",
                      "possibleCauses": ["daily scratches", "washing abrasion"]
                    }
                    """;
        }
    }
}

package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.repository.RepairOrderRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIDiagnosisServiceFusionTest {

    @Test
    void technicianHighConfidenceRuleShouldBeValidatedWithoutSemanticCall() {
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService(0.62);

        AIDiagnosisResponse response = service.diagnoseFault(
                "\u5239\u8f66\u53d8\u8f6f\uff0c\u5239\u8f66\u8ddd\u79bb\u53d8\u957f",
                "technician",
                7L);

        assertEquals(SafetyNetGate.VALIDATED, response.getWorkflowStatus());
        assertNotNull(response.getConfidence());
        assertTrue(response.getConfidence() >= 0.90);
        assertFalse(response.getRuleHits().isEmpty());
        assertTrue(response.getDecisionPath().stream().anyMatch(step -> step.contains("Decision Fusion")));
        assertEquals(0, service.externalCallCount);
    }

    @Test
    void technicianLowConfidenceSemanticResultShouldBeSuspended() {
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService(0.62);

        AIDiagnosisResponse response = service.diagnoseFault(
                "\u6f06\u9762\u8f7b\u5fae\u78e8\u635f\uff0c\u60f3\u5224\u65ad\u662f\u629b\u5149\u8fd8\u662f\u8865\u6f06",
                "technician",
                7L);

        assertEquals(SafetyNetGate.SUSPENDED, response.getWorkflowStatus());
        assertNotNull(response.getConfidence());
        assertTrue(response.getConfidence() < SafetyNetGate.REVIEW_THRESHOLD);
        assertTrue(response.getRuleHits().isEmpty());
        assertTrue(response.getAgentSummaries().stream().anyMatch(summary -> summary.contains("Semantic Agent")));
        assertTrue(response.getDecisionPath().stream().anyMatch(step -> step.contains("manual review required")));
        assertEquals(1, service.externalCallCount);
    }

    @Test
    void customerResponseShouldNotExposeFusionFields() {
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService(0.61);

        AIDiagnosisResponse response = service.diagnoseFault(
                "\u6f06\u9762\u8f7b\u5fae\u78e8\u635f\uff0c\u600e\u4e48\u529e",
                "customer",
                null);

        assertNull(response.getWorkflowStatus());
        assertNull(response.getConfidence());
        assertTrue(response.getRuleHits().isEmpty());
        assertTrue(response.getAgentSummaries().isEmpty());
        assertTrue(response.getDecisionPath().isEmpty());
    }

    private static class CapturingAIDiagnosisService extends AIDiagnosisService {
        private final double semanticConfidence;
        private int externalCallCount;

        private CapturingAIDiagnosisService(double semanticConfidence) {
            super(
                    mock(GamificationService.class),
                    mock(TechnicianService.class),
                    new RuleDiagnosisService(),
                    new PrivacyMaskingService(),
                    new SemanticDiagnosisAgent(),
                    new InventoryDiagnosisAgent(materialServiceWithNoEvidence()),
                    new HistoryCaseAgent(historyRepositoryWithNoEvidence(), new PrivacyMaskingService()),
                    new DecisionFusionEngine(new SafetyNetGate()));
            this.semanticConfidence = semanticConfidence;
        }

        @Override
        protected String callOpenAIAPI(String prompt, String traceId, String stage) throws IOException {
            externalCallCount++;
            return """
                    {
                      "faultType": "paint surface wear",
                      "suggestion": "inspect primer exposure, then choose polishing or localized repainting",
                      "possibleCauses": ["daily scratches", "washing abrasion"],
                      "confidence": %s
                    }
                    """.formatted(semanticConfidence);
        }

        private static MaterialService materialServiceWithNoEvidence() {
            MaterialService materialService = mock(MaterialService.class);
            when(materialService.getAllMaterials()).thenReturn(List.of());
            when(materialService.getActiveInventoryAlerts()).thenReturn(List.of());
            return materialService;
        }

        private static RepairOrderRepository historyRepositoryWithNoEvidence() {
            RepairOrderRepository repository = mock(RepairOrderRepository.class);
            when(repository.findAllWithDetails()).thenReturn(List.of());
            return repository;
        }
    }
}

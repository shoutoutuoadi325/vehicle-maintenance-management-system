package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AIDiagnosisServicePrivacyMaskingTest {

    @Test
    void technicianExternalAiPromptShouldUseMaskedDescription() {
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService();

        AIDiagnosisResponse response = service.diagnoseFault(
                "沪B67890 油漆磨损怎么办，VIN 为 LSVFA49J232123456",
                "technician",
                7L);

        assertTrue(service.capturedPrompts.stream().noneMatch(prompt -> prompt.contains("沪B67890")));
        assertTrue(service.capturedPrompts.stream().noneMatch(prompt -> prompt.contains("LSVFA49J232123456")));
        assertTrue(service.capturedPrompts.stream().anyMatch(prompt -> prompt.contains("[MASKED_PLATE_1]")));
        assertTrue(service.capturedPrompts.stream().anyMatch(prompt -> prompt.contains("[MASKED_VIN_1]")));
        assertTrue(service.capturedStages.contains("AI_CONSILIUM"));
        assertTrue(service.capturedPrompts.get(0).contains("PRE_DIAG"));
        assertTrue(service.capturedPrompts.get(0).contains("ARBITRATOR"));
        assertFalse(service.capturedStages.contains("SEMANTIC_AGENT"));
        assertTrue(response.getDecisionPath().stream().anyMatch(step -> step.contains("隐私脱敏: 已执行")));
    }

    @Test
    void customerExternalAiPromptShouldAlsoUseMaskedDescriptionWithoutDecisionPath() {
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService();

        AIDiagnosisResponse response = service.diagnoseFault(
                "沪B67890 油漆磨损怎么办，VIN 为 LSVFA49J232123456",
                "customer",
                null);

        assertFalse(service.capturedPrompts.get(0).contains("沪B67890"));
        assertFalse(service.capturedPrompts.get(0).contains("LSVFA49J232123456"));
        assertTrue(service.capturedPrompts.get(0).contains("[MASKED_PLATE_1]"));
        assertTrue(service.capturedPrompts.get(0).contains("[MASKED_VIN_1]"));
        assertTrue(service.capturedStages.get(0).equals("SEMANTIC_AGENT"));
        assertTrue(response.getDecisionPath().isEmpty());
    }

    private static class CapturingAIDiagnosisService extends AIDiagnosisService {
        private final List<String> capturedPrompts = new ArrayList<>();
        private final List<String> capturedStages = new ArrayList<>();

        private CapturingAIDiagnosisService() {
            super(
                    mock(GamificationService.class),
                    mock(TechnicianService.class),
                    new RuleDiagnosisService(),
                    new PrivacyMaskingService(),
                    new SemanticDiagnosisAgent(),
                    new InventoryDiagnosisAgent(mock(MaterialService.class)),
                    mock(HistoryCaseAgent.class),
                    new DecisionFusionEngine(new SafetyNetGate()));
        }

        @Override
        protected String callOpenAIAPI(String prompt, java.util.List<String> imageDataUrls, String audioDataUrl, String traceId, String stage) throws IOException {
            this.capturedPrompts.add(prompt);
            this.capturedStages.add(stage);
            return """
                    {
                      "faultType": "漆面磨损",
                      "suggestion": "先清洁漆面并检查是否伤到底漆，再选择抛光、补漆或局部喷涂。",
                      "possibleCauses": ["日常剐蹭", "洗车磨损"]
                    }
                    """;
        }
    }
}

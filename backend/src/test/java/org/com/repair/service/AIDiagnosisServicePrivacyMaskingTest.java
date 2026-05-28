package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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

        assertFalse(service.capturedPrompt.contains("沪B67890"));
        assertFalse(service.capturedPrompt.contains("LSVFA49J232123456"));
        assertTrue(service.capturedPrompt.contains("[MASKED_PLATE_1]"));
        assertTrue(service.capturedPrompt.contains("[MASKED_VIN_1]"));
        assertTrue(service.capturedStage.equals("SEMANTIC_AGENT"));
        assertTrue(response.getDecisionPath().stream().anyMatch(step -> step.contains("隐私脱敏: 已执行")));
    }

    @Test
    void customerExternalAiPromptShouldAlsoUseMaskedDescriptionWithoutDecisionPath() {
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService();

        AIDiagnosisResponse response = service.diagnoseFault(
                "沪B67890 油漆磨损怎么办，VIN 为 LSVFA49J232123456",
                "customer",
                null);

        assertFalse(service.capturedPrompt.contains("沪B67890"));
        assertFalse(service.capturedPrompt.contains("LSVFA49J232123456"));
        assertTrue(service.capturedPrompt.contains("[MASKED_PLATE_1]"));
        assertTrue(service.capturedPrompt.contains("[MASKED_VIN_1]"));
        assertTrue(service.capturedStage.equals("SEMANTIC_AGENT"));
        assertTrue(response.getDecisionPath().isEmpty());
    }

    private static class CapturingAIDiagnosisService extends AIDiagnosisService {
        private String capturedPrompt = "";
        private String capturedStage = "";

        private CapturingAIDiagnosisService() {
            super(
                    mock(GamificationService.class),
                    mock(TechnicianService.class),
                    new RuleDiagnosisService(),
                    new PrivacyMaskingService(),
                    new SemanticDiagnosisAgent(),
                    new InventoryDiagnosisAgent(mock(MaterialService.class)));
        }

        @Override
        protected String callOpenAIAPI(String prompt, String traceId, String stage) throws IOException {
            this.capturedPrompt = prompt;
            this.capturedStage = stage;
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

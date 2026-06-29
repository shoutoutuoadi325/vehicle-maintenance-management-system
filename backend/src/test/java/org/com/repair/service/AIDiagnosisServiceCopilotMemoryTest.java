package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIDiagnosisServiceCopilotMemoryTest {

    @Test
    void technicianConsiliumShouldIncludeAndPersistDedicatedCopilotMemory() {
        TechnicianCopilotMemoryService memoryService = mock(TechnicianCopilotMemoryService.class);
        HistoryCaseAgent historyCaseAgent = mock(HistoryCaseAgent.class);
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService(historyCaseAgent, memoryService);

        when(historyCaseAgent.analyze(any())).thenReturn(new HistoryCaseAgent.HistoryCaseEvidence(
                "History Case Agent: RAG vector retrieval found no similar completed repair cases; tags=HISTORY_NO_SIMILAR_CASE",
                List.of("HISTORY_NO_SIMILAR_CASE")));
        when(memoryService.resolveContext(7L)).thenReturn(new TechnicianCopilotMemoryService.CopilotMemoryContext(
                "Technician Copilot Memory:\n- priorSessions=3\n- lastFaultType=intermittent CAN fault",
                true));

        AIDiagnosisResponse response = service.diagnoseFault(
                "偶发 CAN 总线报错，路试二十分钟后才出现",
                "technician",
                7L);

        assertTrue(service.capturedPrompt.contains("Technician-specific Copilot context"));
        assertTrue(service.capturedPrompt.contains("priorSessions=3"));
        assertTrue(response.getDecisionPath().stream()
                .anyMatch(step -> step.contains("Technician Copilot Memory: loaded persisted technician context")));
        verify(memoryService).recordInteraction(eq(7L), any(), any(AIDiagnosisResponse.class), any());
    }

    private static class CapturingAIDiagnosisService extends AIDiagnosisService {
        private String capturedPrompt = "";

        private CapturingAIDiagnosisService(HistoryCaseAgent historyCaseAgent,
                                            TechnicianCopilotMemoryService memoryService) {
            super(
                    mock(GamificationService.class),
                    mock(TechnicianService.class),
                    new RuleDiagnosisService(),
                    new PrivacyMaskingService(),
                    new SemanticDiagnosisAgent(),
                    new InventoryDiagnosisAgent(materialServiceWithNoEvidence()),
                    historyCaseAgent,
                    new DecisionFusionEngine(new SafetyNetGate()),
                    memoryService);
        }

        @Override
        protected String callOpenAIAPI(String prompt, java.util.List<String> imageDataUrls, String audioDataUrl, String traceId, String stage) throws IOException {
            this.capturedPrompt = prompt;
            return """
                    {
                      "faultType": "intermittent network communication fault",
                      "suggestion": "inspect CAN harness, terminal resistance and module power supply",
                      "possibleCauses": ["connector oxidation", "module intermittent power loss"],
                      "confidence": 0.78
                    }
                    """;
        }

        private static MaterialService materialServiceWithNoEvidence() {
            MaterialService materialService = mock(MaterialService.class);
            when(materialService.getAllMaterials()).thenReturn(List.of());
            when(materialService.getActiveInventoryAlerts()).thenReturn(List.of());
            return materialService;
        }
    }
}

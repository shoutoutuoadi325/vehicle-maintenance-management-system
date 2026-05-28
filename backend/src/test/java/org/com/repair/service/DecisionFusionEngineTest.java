package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionFusionEngineTest {

    private final DecisionFusionEngine engine = new DecisionFusionEngine(new SafetyNetGate());

    @Test
    void highConfidenceRuleUsesRuleOnlyWeight() {
        AIDiagnosisResponse response = new AIDiagnosisResponse("brake fault", "inspect brake system");
        RuleDiagnosisService.RuleDiagnosisResult ruleResult =
                new RuleDiagnosisService.RuleDiagnosisResult(true, true, "brake rule", 0.94, response);

        AIDiagnosisResponse fused = engine.fuse(new DecisionFusionEngine.FusionInput(
                response,
                ruleResult,
                false,
                null,
                null,
                true));

        assertEquals(0.94, fused.getConfidence());
        assertEquals(SafetyNetGate.VALIDATED, fused.getWorkflowStatus());
        assertEquals(List.of("brake rule"), fused.getRuleHits());
        assertTrue(fused.getDecisionPath().stream().anyMatch(step -> step.contains("ruleWeight=100")));
    }

    @Test
    void noRuleLowSemanticConfidenceRequiresManualReview() {
        AIDiagnosisResponse response = new AIDiagnosisResponse("unknown paint issue", "inspect paint surface");
        response.setConfidence(0.62);

        AIDiagnosisResponse fused = engine.fuse(new DecisionFusionEngine.FusionInput(
                response,
                RuleDiagnosisService.RuleDiagnosisResult.noHit(),
                true,
                null,
                null,
                true));

        assertEquals(SafetyNetGate.SUSPENDED, fused.getWorkflowStatus());
        assertTrue(fused.getConfidence() < SafetyNetGate.REVIEW_THRESHOLD);
        assertTrue(fused.getRuleHits().isEmpty());
        assertTrue(fused.getAgentSummaries().contains("Semantic Agent: response parsed"));
    }
}

package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DecisionFusionEngine {

    private static final double HIGH_CONFIDENCE_RULE_WEIGHT = 1.00;
    private static final double RULE_WITH_AGENT_WEIGHT = 0.45;
    private static final double AGENT_WITH_RULE_WEIGHT = 0.55;
    private static final double NO_RULE_WEIGHT = 0.25;
    private static final double AGENT_WITHOUT_RULE_WEIGHT = 0.75;
    private static final double NO_RULE_SIGNAL_CONFIDENCE = 0.50;
    private static final double LOCAL_FALLBACK_CONFIDENCE = 0.60;
    private static final double DEFAULT_SEMANTIC_CONFIDENCE = 0.72;

    private final SafetyNetGate safetyNetGate;

    public DecisionFusionEngine(SafetyNetGate safetyNetGate) {
        this.safetyNetGate = safetyNetGate;
    }

    public AIDiagnosisResponse fuse(FusionInput input) {
        AIDiagnosisResponse response = input.response();
        if (response == null) {
            return response;
        }
        if (!input.technicianRole()) {
            response.setConfidence(null);
            response.setWorkflowStatus(null);
            response.setRuleHits(List.of());
            response.setAgentSummaries(List.of());
            response.setInventoryWarnings(List.of());
            return response;
        }

        List<String> ruleHits = buildRuleHits(input.ruleResult());
        List<String> agentSummaries = buildAgentSummaries(input);
        WeightProfile weights = resolveWeights(input.ruleResult(), input.semanticExecuted());
        double confidence = calculateConfidence(input, weights);
        String workflowStatus = safetyNetGate.evaluate(confidence);

        response.setConfidence(round(confidence));
        response.setWorkflowStatus(workflowStatus);
        response.setRuleHits(ruleHits);
        response.setAgentSummaries(agentSummaries);
        response.setInventoryWarnings(buildInventoryWarnings(input.inventoryEvidence()));
        response.setDecisionPath(buildDecisionPath(response, weights, confidence, workflowStatus));
        return response;
    }

    private List<String> buildRuleHits(RuleDiagnosisService.RuleDiagnosisResult ruleResult) {
        if (ruleResult == null || !ruleResult.matched()) {
            return List.of();
        }
        return List.of(ruleResult.ruleHit());
    }

    private List<String> buildAgentSummaries(FusionInput input) {
        List<String> summaries = new ArrayList<>();
        if (input.semanticExecuted()) {
            if (input.response().getDecisionPath().stream().anyMatch(step -> step.contains("AI Consilium"))) {
                summaries.add("AI Consilium: multi-stage diagnosis parsed");
            } else {
                summaries.add("Semantic Agent: response parsed");
            }
        }
        if (input.inventoryEvidence() != null) {
            summaries.add(input.inventoryEvidence().summary());
        }
        if (input.historyCaseEvidence() != null) {
            summaries.add(input.historyCaseEvidence().summary());
        }
        return summaries;
    }

    private List<String> buildInventoryWarnings(InventoryDiagnosisAgent.InventoryEvidence inventoryEvidence) {
        if (inventoryEvidence == null || inventoryEvidence.lowStockWarnings() == null) {
            return List.of();
        }
        return inventoryEvidence.lowStockWarnings();
    }

    private WeightProfile resolveWeights(RuleDiagnosisService.RuleDiagnosisResult ruleResult, boolean semanticExecuted) {
        if (ruleResult != null && ruleResult.directReturn()) {
            return new WeightProfile(HIGH_CONFIDENCE_RULE_WEIGHT, 0.0);
        }
        if (ruleResult != null && ruleResult.matched()) {
            return semanticExecuted
                    ? new WeightProfile(RULE_WITH_AGENT_WEIGHT, AGENT_WITH_RULE_WEIGHT)
                    : new WeightProfile(HIGH_CONFIDENCE_RULE_WEIGHT, 0.0);
        }
        return semanticExecuted
                ? new WeightProfile(NO_RULE_WEIGHT, AGENT_WITHOUT_RULE_WEIGHT)
                : new WeightProfile(0.0, HIGH_CONFIDENCE_RULE_WEIGHT);
    }

    private double calculateConfidence(FusionInput input, WeightProfile weights) {
        RuleDiagnosisService.RuleDiagnosisResult ruleResult = input.ruleResult();
        double ruleConfidence = ruleResult != null && ruleResult.matched()
                ? ruleResult.confidence()
                : NO_RULE_SIGNAL_CONFIDENCE;
        double agentConfidence = resolveAgentConfidence(input);
        return clamp(ruleConfidence * weights.ruleWeight() + agentConfidence * weights.agentWeight());
    }

    private double resolveAgentConfidence(FusionInput input) {
        AIDiagnosisResponse response = input.response();
        double confidence = input.semanticExecuted()
                ? (response.getConfidence() != null ? response.getConfidence() : DEFAULT_SEMANTIC_CONFIDENCE)
                : LOCAL_FALLBACK_CONFIDENCE;

        if (input.inventoryEvidence() != null && input.inventoryEvidence().hasLowStockRisk()) {
            confidence += 0.04;
        }
        if (input.historyCaseEvidence() != null && input.historyCaseEvidence().hasSimilarCases()) {
            confidence += 0.06;
        }
        return clamp(confidence);
    }

    private List<String> buildDecisionPath(AIDiagnosisResponse response,
                                           WeightProfile weights,
                                           double confidence,
                                           String workflowStatus) {
        List<String> decisionPath = new ArrayList<>(response.getDecisionPath());
        decisionPath.add("Decision Fusion: confidence=" + round(confidence)
                + ", ruleWeight=" + weightPercent(weights.ruleWeight())
                + ", agentWeight=" + weightPercent(weights.agentWeight()));
        if (SafetyNetGate.SUSPENDED.equals(workflowStatus)) {
            decisionPath.add("Safety Net Gate: manual review required; no work order status changed");
        } else {
            decisionPath.add("Safety Net Gate: validated; no automatic work order mutation");
        }
        return decisionPath;
    }

    private int weightPercent(double weight) {
        return (int) Math.round(weight * 100);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record FusionInput(AIDiagnosisResponse response,
                              RuleDiagnosisService.RuleDiagnosisResult ruleResult,
                              boolean semanticExecuted,
                              InventoryDiagnosisAgent.InventoryEvidence inventoryEvidence,
                              HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence,
                              boolean technicianRole) {
    }

    private record WeightProfile(double ruleWeight, double agentWeight) {
    }
}

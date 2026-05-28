package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticDiagnosisAgentTest {

    @Test
    void shouldCallExternalAiAsSemanticAgentAndAppendTechnicianDecisionPath() throws IOException {
        SemanticDiagnosisAgent agent = new SemanticDiagnosisAgent();
        AtomicReference<String> capturedStage = new AtomicReference<>();
        AtomicReference<String> capturedPrompt = new AtomicReference<>();

        AIDiagnosisResponse response = agent.analyze(
                new SemanticDiagnosisAgent.SemanticDiagnosisRequest("冷车启动抖动", "technician", 7L, "trace-1"),
                request -> "semantic prompt for " + request.problemDescription(),
                (prompt, traceId, stage) -> {
                    capturedPrompt.set(prompt);
                    capturedStage.set(stage);
                    return "{\"faultType\":\"点火系统异常\",\"suggestion\":\"读取故障码并检查火花塞\",\"possibleCauses\":[\"点火线圈异常\"]}";
                },
                (responseText, problemDescription) -> {
                    AIDiagnosisResponse parsed = new AIDiagnosisResponse("点火系统异常", responseText);
                    parsed.setPossibleCauses(java.util.List.of(problemDescription));
                    return parsed;
                });

        assertEquals("SEMANTIC_AGENT", capturedStage.get());
        assertEquals("semantic prompt for 冷车启动抖动", capturedPrompt.get());
        assertEquals("点火系统异常", response.getFaultType());
        assertTrue(response.getDecisionPath().stream()
                .anyMatch(step -> step.contains("Semantic Agent: 已执行语义诊断")));
    }

    @Test
    void shouldRejectEmptyExternalAiResponse() {
        SemanticDiagnosisAgent agent = new SemanticDiagnosisAgent();

        assertThrows(IOException.class, () -> agent.analyze(
                new SemanticDiagnosisAgent.SemanticDiagnosisRequest("油漆磨损", "technician", null, "trace-2"),
                request -> "semantic prompt",
                (prompt, traceId, stage) -> " ",
                (responseText, problemDescription) -> new AIDiagnosisResponse("unused", "unused")));
    }

    @Test
    void customerResponseShouldNotExposeDecisionPath() throws IOException {
        SemanticDiagnosisAgent agent = new SemanticDiagnosisAgent();

        AIDiagnosisResponse response = agent.analyze(
                new SemanticDiagnosisAgent.SemanticDiagnosisRequest("油漆磨损", "customer", null, "trace-3"),
                request -> "semantic prompt",
                (prompt, traceId, stage) -> "{\"faultType\":\"漆面磨损\",\"suggestion\":\"先清洁漆面\",\"possibleCauses\":[]}",
                (responseText, problemDescription) -> new AIDiagnosisResponse("漆面磨损", responseText));

        assertTrue(response.getDecisionPath().isEmpty());
    }
}

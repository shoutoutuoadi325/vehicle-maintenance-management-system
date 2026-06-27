package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SemanticDiagnosisAgent {

    private static final String STAGE = "SEMANTIC_AGENT";

    public AIDiagnosisResponse analyze(SemanticDiagnosisRequest request,
                                       PromptComposer promptComposer,
                                       ExternalAiCaller externalAiCaller,
                                       ResponseParser responseParser) throws IOException {
        String prompt = promptComposer.compose(request);
        String diagnosisText = externalAiCaller.call(prompt, request.imageDataUrls(), request.audioDataUrl(), request.traceId(), STAGE);
        if (diagnosisText == null || diagnosisText.isBlank()) {
            throw new IOException("Semantic Agent returned empty diagnosis content");
        }

        AIDiagnosisResponse response = responseParser.parse(diagnosisText, request.problemDescription());
        appendDecisionPath(response, request.role());
        return response;
    }

    private void appendDecisionPath(AIDiagnosisResponse response, String role) {
        if (response == null || !"technician".equals(role)) {
            return;
        }

        List<String> decisionPath = new ArrayList<>(response.getDecisionPath());
        decisionPath.add("Semantic Agent: 已执行语义诊断");
        response.setDecisionPath(decisionPath);
    }

    public record SemanticDiagnosisRequest(String problemDescription,
                                           String role,
                                           Long technicianId,
                                           String traceId,
                                           List<String> imageDataUrls,
                                           String audioDataUrl) {
    }

    @FunctionalInterface
    public interface PromptComposer {
        String compose(SemanticDiagnosisRequest request);
    }

    @FunctionalInterface
    public interface ExternalAiCaller {
        String call(String prompt, List<String> imageDataUrls, String audioDataUrl, String traceId, String stage) throws IOException;
    }

    @FunctionalInterface
    public interface ResponseParser {
        AIDiagnosisResponse parse(String responseText, String problemDescription);
    }
}

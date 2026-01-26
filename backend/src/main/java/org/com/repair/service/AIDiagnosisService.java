package org.com.repair.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.com.repair.DTO.AIDiagnosisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class AIDiagnosisService {

    private static final Logger logger = LoggerFactory.getLogger(AIDiagnosisService.class);

    @Value("${ai.diagnosis.api.key}")
    private String apiKey;

    @Value("${ai.diagnosis.api.base-url}")
    private String baseUrl;

    @Value("${ai.diagnosis.api.model}")
    private String model;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public AIDiagnosisService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription) {
        try {
            String prompt = buildPrompt(problemDescription);
            String responseText = callOpenAIAPI(prompt);
            return parseResponse(responseText);
        } catch (Exception e) {
            logger.error("Error during AI diagnosis: ", e);
            return new AIDiagnosisResponse("AI诊断服务暂时不可用，请稍后再试");
        }
    }

    private String buildPrompt(String problemDescription) {
        return "你是一个汽车维修专家。请根据以下问题描述，诊断可能的故障类型并给出建议。\n\n" +
                "问题描述：" + problemDescription + "\n\n" +
                "请用以下格式回复：\n" +
                "故障类型：[具体的故障类型]\n" +
                "建议：[详细的维修建议]";
    }

    private String callOpenAIAPI(String prompt) throws IOException {
        String requestBody = String.format(
            "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7}",
            model,
            prompt.replace("\"", "\\\"").replace("\n", "\\n")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                throw new IOException("API调用失败，状态码: " + response.code() + ", 响应: " + responseBody);
            }

            logger.info("API Response: {}", responseBody);
            
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.path("choices").get(0).path("message").path("content").asText();
        }
    }

    private AIDiagnosisResponse parseResponse(String responseText) {
        // 解析AI返回的文本
        String faultType = "";
        String suggestion = "";

        String[] lines = responseText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("故障类型：") || line.startsWith("故障类型:")) {
                faultType = line.substring(5).trim();
            } else if (line.startsWith("建议：") || line.startsWith("建议:")) {
                suggestion = line.substring(3).trim();
            }
        }

        // 如果没有按格式返回，使用整个响应作为建议
        if (faultType.isEmpty() && suggestion.isEmpty()) {
            suggestion = responseText;
            faultType = "综合诊断";
        }

        return new AIDiagnosisResponse(faultType, suggestion);
    }
}

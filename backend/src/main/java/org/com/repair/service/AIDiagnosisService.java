package org.com.repair.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.service.TechnicianService.TechnicianFatigueSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AIDiagnosisService {

    private static final Logger logger = LoggerFactory.getLogger(AIDiagnosisService.class);
    private static final String PRE_DIAG_PROMPT = "你是一个预诊客服。请从车主的描述中提取关键信息，严格输出JSON格式的故障特征表，包含：核心症状、疑似部位、触发条件。不要给出最终结论。";
    private static final String MAIN_AGENT_PROMPT = "你是主治维修工程师。请根据以下结构化特征表，给出初步排查路径和概率最高的三个故障点。";
    private static final String RED_TEAM_PROMPT = "你是专挑刺的QA工程师（红队）。请仔细审视主治工程师的结论，指出他可能忽略的低概率/高风险长尾故障，或者排查逻辑中的漏洞。";
    private static final String ARBITRATOR_PROMPT = "你是总控车间主任。请综合【主治诊断】和【红队质疑】，输出最终的综合问诊及排查指导报告。格式需清晰易读。";

    @Value("${ai.diagnosis.api.key}")
    private String apiKey;

    @Value("${ai.diagnosis.api.base-url}")
    private String baseUrl;

    @Value("${ai.diagnosis.api.model}")
    private String model;

    private final GamificationService gamificationService;
    private final TechnicianService technicianService;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public AIDiagnosisService(GamificationService gamificationService,
                              TechnicianService technicianService) {
        this.gamificationService = gamificationService;
        this.technicianService = technicianService;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription, String role) {
        return diagnoseFault(problemDescription, role, null);
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription, String role, Long technicianId) {
        String traceId = UUID.randomUUID().toString();
        String normalizedRole = normalizeRole(role);
        logger.info("[AI-CONSILIUM][{}] 会诊开始, role={}, technicianId={}", traceId, normalizedRole, technicianId);
        try {
            AIDiagnosisResponse response = expertConsilium(problemDescription, normalizedRole, technicianId, traceId);
            logger.info("[AI-CONSILIUM][{}] 会诊完成", traceId);
            return response;
        } catch (Exception e) {
            logger.warn("[AI-CONSILIUM][{}] 专家会诊失败，回退单模型诊断: {}", traceId, e.getMessage(), e);
            try {
                String fallbackPrompt = buildPrompt(problemDescription, normalizedRole);
                String fallbackText = callOpenAIAPI(fallbackPrompt, traceId, "FALLBACK_SINGLE");
                AIDiagnosisResponse fallbackResponse = parseResponse(fallbackText);
                logger.info("[AI-CONSILIUM][{}] 单模型回退完成", traceId);
                return fallbackResponse;
            } catch (SocketTimeoutException timeoutException) {
                logger.error("[AI-CONSILIUM][{}] AI诊断回退超时", traceId, timeoutException);
                return new AIDiagnosisResponse("专家会诊超时，请稍后再试");
            } catch (Exception fallbackException) {
                logger.error("[AI-CONSILIUM][{}] AI诊断回退失败", traceId, fallbackException);
                return new AIDiagnosisResponse("AI诊断服务暂时不可用，请稍后再试");
            }
        }
    }

    private AIDiagnosisResponse expertConsilium(String problemDescription, String role, Long technicianId, String traceId) throws IOException {
        List<String> ecoTips = gamificationService.getEcoWaitingTips();
        String ecoTipContext = ecoTips.isEmpty() ? "" : ("\n\n绿色导向参考：" + ecoTips.get(0));
        double fatigueLevel = resolveTechnicianFatigueLevel(technicianId, traceId);

        logger.info("[AI-CONSILIUM][{}] 融合上下文: ecoTips={}, fatigueLevel={}", traceId, ecoTips.size(), fatigueLevel);

        String preDiagInput = PRE_DIAG_PROMPT + "\n\n车主描述：" + problemDescription + ecoTipContext;
        String structuredFeatures = runAgentStep("PRE_DIAG", preDiagInput, traceId);

        String mainAgentInput = MAIN_AGENT_PROMPT +
                "\n\n角色上下文：" + role +
                "\n\n结构化特征表：\n" + structuredFeatures;
        String initialDiagnosis = runAgentStep("MAIN_AGENT", mainAgentInput, traceId);

        String redTeamInput = RED_TEAM_PROMPT +
                "\n\n结构化特征表：\n" + structuredFeatures +
                "\n\n主治诊断：\n" + initialDiagnosis;
        String critique = runAgentStep("RED_TEAM", redTeamInput, traceId);

        String arbitratorInput = ARBITRATOR_PROMPT +
                "\n\n结构化特征表：\n" + structuredFeatures +
                "\n\n【主治诊断】\n" + initialDiagnosis +
                "\n\n【红队质疑】\n" + critique +
                "\n\n当前被派单技师的疲劳度指标为：" + fatigueLevel + " (0到1之间)。" +
                "\n极其重要的动态安全规则：如果该指标大于 0.7，你必须在报告中将原本复杂的排查步骤进行最细粒度的'傻瓜式'拆解，并在涉及高压电、燃油等高危操作前，强制插入带有 [安全高危校验] 标签的物理确认步骤。如果小于 0.7，则正常输出。" +
                "\n\n最终输出必须使用 Markdown 格式，至少包含：\n" +
                "- `## 综合结论`\n" +
                "- `## 分步排查`\n" +
                "- `## 风险与安全提示`\n" +
                "- 必要时在步骤中插入 `[安全高危校验]` 标记。";
        String finalReport = runAgentStep("ARBITRATOR", arbitratorInput, traceId);

        return new AIDiagnosisResponse("专家会诊", finalReport);
    }

    private double resolveTechnicianFatigueLevel(Long technicianId, String traceId) {
        double defaultFatigueLevel = 0.8;
        if (technicianId == null) {
            logger.info("[AI-CONSILIUM][{}] 未提供技师ID，使用默认疲劳度={}", traceId, defaultFatigueLevel);
            return defaultFatigueLevel;
        }

        try {
            TechnicianFatigueSnapshot snapshot = technicianService.getTechnicianFatigueSnapshot(technicianId);
            double fatigueLevel = snapshot.getFatigueLevel();

            logger.info("[AI-CONSILIUM][{}] 技师疲劳度计算成功, technicianId={}, completedToday={}, continuousHours={}, nightOrdersToday={}, fatigue={}",
                    traceId,
                    technicianId,
                    snapshot.getCompletedToday(),
                    snapshot.getContinuousWorkingHours(),
                    snapshot.getNightOrdersToday(),
                    fatigueLevel);
            return fatigueLevel;
        } catch (Exception ex) {
            logger.warn("[AI-CONSILIUM][{}] 技师疲劳度估算失败，回退默认值: {}", traceId, ex.getMessage());
            return defaultFatigueLevel;
        }
    }

    private String runAgentStep(String stage, String prompt, String traceId) throws IOException {
        logger.info("[AI-CONSILIUM][{}][{}] 调用开始, promptLength={}", traceId, stage, prompt != null ? prompt.length() : 0);
        String result = callOpenAIAPI(prompt, traceId, stage);
        if (result == null || result.isBlank()) {
            throw new IOException(stage + " 阶段返回空结果");
        }
        logger.info("[AI-CONSILIUM][{}][{}] 调用完成, resultLength={}", traceId, stage, result.length());
        return result;
    }

    private String buildPrompt(String problemDescription, String role) {
        String normalizedRole = normalizeRole(role);
        if ("technician".equals(normalizedRole)) {
            return "你是一名资深汽车诊断技师 AI 副驾。请基于技师实战场景输出专业、可执行的排查方案。\n\n" +
                    "问题描述：" + problemDescription + "\n\n" +
                    "输出要求：\n" +
                    "1. 先给出最可能的故障类型（可列 1-3 个，按概率排序）。\n" +
                    "2. 给出分步骤排查流程（从低成本、低风险到高成本验证）。\n" +
                    "3. 指出建议使用的工具/检测数据（例如 OBD 故障码、万用表、电压、电阻、波形等）。\n" +
                    "4. 给出维修建议与风险提示（含可能误判点）。\n" +
                    "请用以下格式回复：\n" +
                    "故障类型：[具体故障类型，必要时列多个]\n" +
                    "建议：[分点给出排查与维修建议]";
        }

        return "你是一个面向车主的汽车维修顾问。请根据以下问题描述，给出易懂的故障判断和建议。\n\n" +
                "问题描述：" + problemDescription + "\n\n" +
                "输出要求：\n" +
                "1. 使用非专业用户易懂的语言。\n" +
                "2. 先说明可能故障，再给出下一步建议（是否继续行驶、是否立即检修）。\n" +
                "3. 避免过于复杂术语，必要时简要解释。\n" +
                "请用以下格式回复：\n" +
                "故障类型：[具体的故障类型]\n" +
                "建议：[详细的维修建议]";
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "customer";
        }
        return role.trim().toLowerCase();
    }

    private String callOpenAIAPI(String prompt, String traceId, String stage) throws IOException {
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

            logger.info("[AI-CONSILIUM][{}][{}] API成功, status={}, responseLength={}",
                    traceId,
                    stage,
                    response.code(),
                    responseBody != null ? responseBody.length() : 0);
            
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

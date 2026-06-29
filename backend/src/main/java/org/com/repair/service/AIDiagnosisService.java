package org.com.repair.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.service.TechnicianService.TechnicianFatigueSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private final RuleDiagnosisService ruleDiagnosisService;
    private final PrivacyMaskingService privacyMaskingService;
    private final SemanticDiagnosisAgent semanticDiagnosisAgent;
    private final InventoryDiagnosisAgent inventoryDiagnosisAgent;
    private final HistoryCaseAgent historyCaseAgent;
    private final DecisionFusionEngine decisionFusionEngine;
    private final TechnicianCopilotMemoryService technicianCopilotMemoryService;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public AIDiagnosisService(GamificationService gamificationService,
                              TechnicianService technicianService,
                              RuleDiagnosisService ruleDiagnosisService,
                              PrivacyMaskingService privacyMaskingService,
                              SemanticDiagnosisAgent semanticDiagnosisAgent,
                              InventoryDiagnosisAgent inventoryDiagnosisAgent,
                              HistoryCaseAgent historyCaseAgent,
                              DecisionFusionEngine decisionFusionEngine) {
        this(gamificationService,
                technicianService,
                ruleDiagnosisService,
                privacyMaskingService,
                semanticDiagnosisAgent,
                inventoryDiagnosisAgent,
                historyCaseAgent,
                decisionFusionEngine,
                null);
    }

    @Autowired
    public AIDiagnosisService(GamificationService gamificationService,
                              TechnicianService technicianService,
                              RuleDiagnosisService ruleDiagnosisService,
                              PrivacyMaskingService privacyMaskingService,
                              SemanticDiagnosisAgent semanticDiagnosisAgent,
                              InventoryDiagnosisAgent inventoryDiagnosisAgent,
                              HistoryCaseAgent historyCaseAgent,
                              DecisionFusionEngine decisionFusionEngine,
                              TechnicianCopilotMemoryService technicianCopilotMemoryService) {
        this.gamificationService = gamificationService;
        this.technicianService = technicianService;
        this.ruleDiagnosisService = ruleDiagnosisService;
        this.privacyMaskingService = privacyMaskingService;
        this.semanticDiagnosisAgent = semanticDiagnosisAgent;
        this.inventoryDiagnosisAgent = inventoryDiagnosisAgent;
        this.historyCaseAgent = historyCaseAgent;
        this.decisionFusionEngine = decisionFusionEngine;
        this.technicianCopilotMemoryService = technicianCopilotMemoryService;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription, String role) {
        return diagnoseFault(problemDescription, role, null);
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription, String role, Long technicianId) {
        return diagnoseFault(problemDescription, role, technicianId, null, null);
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription,
                                             String role,
                                             Long technicianId,
                                             List<String> imageDataUrls) {
        return diagnoseFault(problemDescription, role, technicianId, imageDataUrls, null);
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription,
                                             String role,
                                             Long technicianId,
                                             List<String> imageDataUrls,
                                             String audioDataUrl) {
        String traceId = UUID.randomUUID().toString();
        String normalizedRole = normalizeRole(role);
        String normalizedDescription = problemDescription == null ? "" : problemDescription.trim();
        logger.info("[AI-DIAGNOSIS][{}] 诊断开始, role={}, technicianId={}, hasImages={}, hasAudio={}", traceId, normalizedRole, technicianId, imageDataUrls != null && !imageDataUrls.isEmpty(), audioDataUrl != null && !audioDataUrl.isBlank());
        RuleDiagnosisService.RuleDiagnosisResult ruleResult = RuleDiagnosisService.RuleDiagnosisResult.noHit();
        PrivacyMaskingService.MaskingResult maskingResult = privacyMaskingService.mask(normalizedDescription);
        InventoryDiagnosisAgent.InventoryEvidence inventoryEvidence = null;
        HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence = null;
        TechnicianCopilotMemoryService.CopilotMemoryContext copilotMemoryContext =
                TechnicianCopilotMemoryService.CopilotMemoryContext.empty();
        try {
            if (isTechnicianRole(normalizedRole)) {
                ruleResult = ruleDiagnosisService.diagnose(normalizedDescription);
                inventoryEvidence = inventoryDiagnosisAgent.analyze(normalizedDescription);
                historyCaseEvidence = historyCaseAgent.analyze(normalizedDescription);
                copilotMemoryContext = resolveCopilotMemoryContext(technicianId, traceId);
                if (ruleResult.directReturn()) {
                    logger.info("[AI-DIAGNOSIS][{}] 技师端规则高置信直出, ruleHit={}, confidence={}",
                            traceId,
                            ruleResult.ruleHit(),
                            ruleResult.confidence());
                    AIDiagnosisResponse response = ruleResult.response();
                    AIDiagnosisResponse finalResponse = finalizeDiagnosisResponse(
                            response, ruleResult, false, inventoryEvidence, historyCaseEvidence, maskingResult, normalizedRole);
                    appendCopilotMemoryDecisionPath(finalResponse, copilotMemoryContext, normalizedRole);
                    recordCopilotMemory(technicianId, normalizedDescription, finalResponse, historyCaseEvidence, normalizedRole, traceId);
                    return finalResponse;
                }
            }
            AIDiagnosisResponse response;
            if (isTechnicianRole(normalizedRole)) {
                response = expertConsilium(
                        maskingResult.maskedText(),
                        normalizedRole,
                        technicianId,
                        traceId,
                        inventoryEvidence,
                        historyCaseEvidence,
                        copilotMemoryContext,
                        imageDataUrls,
                        audioDataUrl);
            } else {
                response = externalSingleDiagnosis(
                        maskingResult.maskedText(),
                        normalizedRole,
                        technicianId,
                        traceId,
                        inventoryEvidence,
                        historyCaseEvidence,
                        imageDataUrls,
                        audioDataUrl);
            }
            logger.info("[AI-DIAGNOSIS][{}] 外部AI诊断完成", traceId);
            AIDiagnosisResponse finalResponse = finalizeDiagnosisResponse(
                    response, ruleResult, true, inventoryEvidence, historyCaseEvidence, maskingResult, normalizedRole);
            appendCopilotMemoryDecisionPath(finalResponse, copilotMemoryContext, normalizedRole);
            recordCopilotMemory(technicianId, normalizedDescription, finalResponse, historyCaseEvidence, normalizedRole, traceId);
            return finalResponse;
        } catch (Exception e) {
            if (isTechnicianRole(normalizedRole) && ruleResult.matched()) {
                logger.warn("[AI-DIAGNOSIS][{}] 外部AI诊断失败，返回已命中的技师端规则结果: ruleHit={}, confidence={}, error={}",
                        traceId,
                        ruleResult.ruleHit(),
                        ruleResult.confidence(),
                        e.getMessage());
                AIDiagnosisResponse response = ruleResult.response();
                AIDiagnosisResponse finalResponse = finalizeDiagnosisResponse(
                        response, ruleResult, false, inventoryEvidence, historyCaseEvidence, maskingResult, normalizedRole);
                appendCopilotMemoryDecisionPath(finalResponse, copilotMemoryContext, normalizedRole);
                recordCopilotMemory(technicianId, normalizedDescription, finalResponse, historyCaseEvidence, normalizedRole, traceId);
                return finalResponse;
            }
            logger.warn("[AI-DIAGNOSIS][{}] 外部AI诊断失败，启用本地规则兜底: {}", traceId, e.getMessage(), e);
            AIDiagnosisResponse response = buildLocalFallbackResponse(normalizedDescription, normalizedRole);
            AIDiagnosisResponse finalResponse = finalizeDiagnosisResponse(
                    response, ruleResult, false, inventoryEvidence, historyCaseEvidence, maskingResult, normalizedRole);
            appendCopilotMemoryDecisionPath(finalResponse, copilotMemoryContext, normalizedRole);
            recordCopilotMemory(technicianId, normalizedDescription, finalResponse, historyCaseEvidence, normalizedRole, traceId);
            return finalResponse;
        }
    }
    // Image fallback handling integrated into catch block of diagnoseFault



    private AIDiagnosisResponse externalSingleDiagnosis(String problemDescription,
                                                        String role,
                                                        Long technicianId,
                                                        String traceId,
                                                        InventoryDiagnosisAgent.InventoryEvidence inventoryEvidence,
                                                        HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence,
                                                        List<String> imageDataUrls,
                                                        String audioDataUrl) throws IOException {
        return semanticDiagnosisAgent.analyze(
                new SemanticDiagnosisAgent.SemanticDiagnosisRequest(problemDescription, role, technicianId, traceId, imageDataUrls, audioDataUrl),
                request -> buildPrompt(request.problemDescription(), request.role())
                        + buildOperationalContext(request.technicianId(), request.traceId())
                        + buildInventoryPromptContext(inventoryEvidence, request.role())
                        + buildHistoryCasePromptContext(historyCaseEvidence, request.role())
                        + semanticJsonOutputContract(),
                this::callOpenAIAPI,
                this::parseResponse);
    }

    private String buildInventoryPromptContext(InventoryDiagnosisAgent.InventoryEvidence inventoryEvidence, String role) {
        if (!isTechnicianRole(role) || inventoryEvidence == null) {
            return "";
        }
        return "\n\nInventory Agent context:" + inventoryEvidence.promptContext();
    }

    private String buildHistoryCasePromptContext(HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence, String role) {
        if (!isTechnicianRole(role) || historyCaseEvidence == null) {
            return "";
        }
        return "\n\nHistory Case Agent context:" + historyCaseEvidence.promptContext();
    }

    private String buildCopilotMemoryPromptContext(TechnicianCopilotMemoryService.CopilotMemoryContext copilotMemoryContext,
                                                   String role) {
        if (!isTechnicianRole(role)
                || copilotMemoryContext == null
                || copilotMemoryContext.promptContext() == null
                || copilotMemoryContext.promptContext().isBlank()) {
            return "";
        }
        return "\n\nTechnician-specific Copilot context:\n- " + copilotMemoryContext.promptContext();
    }

    private AIDiagnosisResponse finalizeDiagnosisResponse(AIDiagnosisResponse response,
                                                          RuleDiagnosisService.RuleDiagnosisResult ruleResult,
                                                          boolean semanticExecuted,
                                                          InventoryDiagnosisAgent.InventoryEvidence inventoryEvidence,
                                                          HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence,
                                                          PrivacyMaskingService.MaskingResult maskingResult,
                                                          String normalizedRole) {
        appendInventoryDecisionPath(response, inventoryEvidence, normalizedRole);
        appendHistoryCaseDecisionPath(response, historyCaseEvidence, normalizedRole);
        appendPrivacyDecisionPath(response, maskingResult, normalizedRole);
        return decisionFusionEngine.fuse(new DecisionFusionEngine.FusionInput(
                response,
                ruleResult,
                semanticExecuted,
                inventoryEvidence,
                historyCaseEvidence,
                isTechnicianRole(normalizedRole)));
    }

    private TechnicianCopilotMemoryService.CopilotMemoryContext resolveCopilotMemoryContext(Long technicianId,
                                                                                            String traceId) {
        if (technicianCopilotMemoryService == null) {
            return TechnicianCopilotMemoryService.CopilotMemoryContext.empty();
        }
        TechnicianCopilotMemoryService.CopilotMemoryContext context =
                technicianCopilotMemoryService.resolveContext(technicianId);
        logger.info("[AI-COPILOT][{}] 专属记忆上下文加载完成, technicianId={}, found={}",
                traceId,
                technicianId,
                context.found());
        return context;
    }

    private void recordCopilotMemory(Long technicianId,
                                     String problemDescription,
                                     AIDiagnosisResponse response,
                                     HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence,
                                     String normalizedRole,
                                     String traceId) {
        if (!isTechnicianRole(normalizedRole) || technicianCopilotMemoryService == null) {
            return;
        }
        technicianCopilotMemoryService.recordInteraction(technicianId, problemDescription, response, historyCaseEvidence);
        logger.info("[AI-COPILOT][{}] 专属记忆写回完成, technicianId={}", traceId, technicianId);
    }

    private void appendCopilotMemoryDecisionPath(AIDiagnosisResponse response,
                                                 TechnicianCopilotMemoryService.CopilotMemoryContext copilotMemoryContext,
                                                 String normalizedRole) {
        if (!isTechnicianRole(normalizedRole) || response == null || copilotMemoryContext == null) {
            return;
        }
        java.util.List<String> decisionPath = new java.util.ArrayList<>(response.getDecisionPath());
        decisionPath.add(copilotMemoryContext.found()
                ? "Technician Copilot Memory: loaded persisted technician context"
                : "Technician Copilot Memory: no persisted technician context");
        response.setDecisionPath(decisionPath);
    }

    private String semanticJsonOutputContract() {
        return "\n\n请只输出一个 JSON 对象，不要输出 Markdown，不要输出代码块，不要照抄字段说明。\n"
                + "JSON 字段必须为：\n"
                + "{\n"
                + "  \"faultType\": \"用一句话写出1-3个最可能故障，不要写'故障类型'四个字\",\n"
                + "  \"suggestion\": \"写出具体排查步骤、是否建议继续行驶、维修风险和安全提醒，不要写'建议'两个字\",\n"
                + "  \"possibleCauses\": [\"原因1\", \"原因2\", \"原因3\"]\n"
                + "}";
    }

    private String buildOperationalContext(Long technicianId, String traceId) {
        StringBuilder context = new StringBuilder("\n\n业务上下文：");

        try {
            List<String> ecoTips = gamificationService.getEcoWaitingTips();
            if (ecoTips != null && !ecoTips.isEmpty()) {
                context.append("\n- 绿色维修提示：").append(ecoTips.get(0));
            }
        } catch (Exception ex) {
            logger.warn("[AI-DIAGNOSIS][{}] 获取绿色维修上下文失败，忽略该上下文: {}", traceId, ex.getMessage());
        }

        double fatigueLevel = resolveTechnicianFatigueLevel(technicianId, traceId);
        context.append("\n- 当前技师疲劳度：").append(fatigueLevel).append("（0到1之间）");
        if (fatigueLevel > 0.7) {
            context.append("\n- 若涉及高压电、燃油、制动或高温部件，请把安全校验步骤写得更明确。");
        }

        return context.toString();
    }

    private AIDiagnosisResponse expertConsilium(String problemDescription,
                                                String role,
                                                Long technicianId,
                                                String traceId,
                                                InventoryDiagnosisAgent.InventoryEvidence inventoryEvidence,
                                                HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence,
                                                TechnicianCopilotMemoryService.CopilotMemoryContext copilotMemoryContext,
                                                List<String> imageDataUrls,
                                                String audioDataUrl) throws IOException {
        List<String> ecoTips = resolveEcoTips(traceId);
        String ecoTipContext = ecoTips.isEmpty() ? "" : ("\n\n绿色导向参考：" + ecoTips.get(0));
        double fatigueLevel = resolveTechnicianFatigueLevel(technicianId, traceId);

        logger.info("[AI-CONSILIUM][{}] 融合上下文: ecoTips={}, fatigueLevel={}", traceId, ecoTips.size(), fatigueLevel);

        String consiliumInput = "你是一个车辆维修多 Agent 协同诊断系统。请在一次响应中完成以下内部协作流程：\n"
                + "1. PRE_DIAG: " + PRE_DIAG_PROMPT + "\n"
                + "2. MAIN_AGENT: " + MAIN_AGENT_PROMPT + "\n"
                + "3. RED_TEAM: " + RED_TEAM_PROMPT + "\n"
                + "4. ARBITRATOR: " + ARBITRATOR_PROMPT + "\n\n"
                + "车主描述：" + problemDescription
                + (hasMediaEvidence(imageDataUrls, audioDataUrl) ? "\n\n如有图片或语音证据，请在 PRE_DIAG 阶段一并提取可见/可听线索。" : "")
                + "\n\n角色上下文：" + role
                + ecoTipContext
                + buildInventoryPromptContext(inventoryEvidence, role)
                + buildHistoryCasePromptContext(historyCaseEvidence, role)
                + buildCopilotMemoryPromptContext(copilotMemoryContext, role)
                + "\n\n当前被派单技师的疲劳度指标为：" + fatigueLevel + " (0到1之间)。"
                + "\n极其重要的动态安全规则：如果该指标大于 0.7，你必须在报告中将原本复杂的排查步骤进行最细粒度拆解，并在涉及高压电、燃油等高危操作前，强制插入带有 [安全高危校验] 标签的物理确认步骤。如果小于 0.7，则正常输出。"
                + "\n\n输出要求："
                + "\n- 你必须先在内部完成 PRE_DIAG、MAIN_AGENT、RED_TEAM、ARBITRATOR 四阶段协作，但最终不要输出内部草稿。"
                + "\n- suggestion 字段中用 Markdown 写出 `## 综合结论`、`## 分步排查`、`## 风险与安全提示`。"
                + "\n- 请只输出一个 JSON 对象，不要输出 Markdown 代码块。"
                + "\n{\n"
                + "  \"faultType\": \"用一句话写出1-3个最可能故障\",\n"
                + "  \"suggestion\": \"最终综合报告，包含分步排查、风险与安全提示\",\n"
                + "  \"possibleCauses\": [\"原因1\", \"原因2\", \"原因3\"],\n"
                + "  \"confidence\": 0.0\n"
                + "}";
        String finalReport = runAgentStep("AI_CONSILIUM", consiliumInput, traceId, imageDataUrls, audioDataUrl);

        AIDiagnosisResponse response = parseResponse(finalReport, problemDescription);
        java.util.List<String> decisionPath = new java.util.ArrayList<>(response.getDecisionPath());
        decisionPath.add("AI Consilium: PRE_DIAG -> MAIN_AGENT -> RED_TEAM -> ARBITRATOR (single external round-trip)");
        response.setDecisionPath(decisionPath);
        return response;
    }

    private List<String> resolveEcoTips(String traceId) {
        try {
            List<String> ecoTips = gamificationService.getEcoWaitingTips();
            return ecoTips == null ? List.of() : ecoTips;
        } catch (Exception ex) {
            logger.warn("[AI-CONSILIUM][{}] 获取绿色维修上下文失败，忽略该上下文: {}", traceId, ex.getMessage());
            return List.of();
        }
    }

    private double resolveTechnicianFatigueLevel(Long technicianId, String traceId) {
        double defaultFatigueLevel = 0.8;
        if (technicianId == null) {
            logger.info("[AI-CONSILIUM][{}] 未提供技师ID，使用默认疲劳度={}", traceId, defaultFatigueLevel);
            return defaultFatigueLevel;
        }

        try {
            TechnicianFatigueSnapshot snapshot = technicianService.getTechnicianFatigueSnapshot(technicianId);
            if (snapshot == null) {
                logger.info("[AI-CONSILIUM][{}] 未获取到技师疲劳度快照，使用默认疲劳度={}", traceId, defaultFatigueLevel);
                return defaultFatigueLevel;
            }
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
            logger.warn("[AI-CONSILIUM][{}] 技师疲劳度估算失败，回退默认值: {}", traceId, ex.getMessage(), ex);
            return defaultFatigueLevel;
        }
    }

    private String runAgentStep(String stage, String prompt, String traceId) throws IOException {
        return runAgentStep(stage, prompt, traceId, null, null);
    }

    private String runAgentStep(String stage,
                                String prompt,
                                String traceId,
                                List<String> imageDataUrls,
                                String audioDataUrl) throws IOException {
        logger.info("[AI-CONSILIUM][{}][{}] 调用开始, promptLength={}", traceId, stage, prompt != null ? prompt.length() : 0);
        String result = callOpenAIAPI(prompt, imageDataUrls, audioDataUrl, traceId, stage);
        if (result == null || result.isBlank()) {
            throw new IOException(stage + " 阶段返回空结果");
        }
        logger.info("[AI-CONSILIUM][{}][{}] 调用完成, resultLength={}", traceId, stage, result.length());
        return result;
    }

    private boolean hasMediaEvidence(List<String> imageDataUrls, String audioDataUrl) {
        return (imageDataUrls != null && !imageDataUrls.isEmpty())
                || (audioDataUrl != null && !audioDataUrl.isBlank());
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
                    "4. 给出维修建议与风险提示（含可能误判点）。";
        }

        return "你是一个面向车主的汽车维修顾问。请根据以下问题描述，给出易懂的故障判断和建议。\n\n" +
                "问题描述：" + problemDescription + "\n\n" +
                "输出要求：\n" +
                "1. 使用非专业用户易懂的语言。\n" +
                "2. 先说明可能故障，再给出下一步建议（是否继续行驶、是否立即检修）。\n" +
                "3. 避免过于复杂术语，必要时简要解释。";
    }

    private AIDiagnosisResponse buildLocalFallbackResponse(String problemDescription, String role) {
        String text = problemDescription == null ? "" : problemDescription.trim();
        String normalized = text.toLowerCase();
        String faultType = "综合故障初诊";
        java.util.List<String> causes = new java.util.ArrayList<>();
        java.util.List<String> steps = new java.util.ArrayList<>();

        if (containsAny(normalized, "无法启动", "启动困难", "打不着", "点不着", "熄火")) {
            faultType = "启动系统/点火供油故障";
            causes.add("蓄电池电量不足或接线柱松动、氧化");
            causes.add("起动机、点火线圈、火花塞或燃油泵工作异常");
            causes.add("曲轴位置传感器、节气门或相关保险丝/继电器异常");
            steps.add("先检查电瓶电压、接线柱和搭铁线，确认启动瞬间电压是否明显跌落");
            steps.add("读取 OBD 故障码，重点关注点火、燃油压力、曲轴/凸轮轴传感器相关报码");
            steps.add("检查火花塞点火状态、燃油泵工作声和油压，按低成本到高成本顺序排查");
        } else if (containsAny(normalized, "刹车", "制动", "刹不住", "刹车软", "刹车异响")) {
            faultType = "制动系统故障";
            causes.add("刹车片/刹车盘磨损或表面异常");
            causes.add("制动液不足、含水率过高或管路进气");
            causes.add("制动分泵、总泵或 ABS 相关部件异常");
            steps.add("立即降低车速并避免继续高速行驶，优先检查制动液液位和泄漏痕迹");
            steps.add("检查刹车片厚度、刹车盘沟槽和轮端是否有异常发热");
            steps.add("读取 ABS/ESP 故障码，必要时进行制动管路排气和制动压力测试");
        } else if (containsAny(normalized, "高温", "水温", "开锅", "冷却液", "防冻液")) {
            faultType = "冷却系统过热故障";
            causes.add("冷却液不足、管路泄漏或水箱散热不良");
            causes.add("节温器、水泵或电子风扇工作异常");
            causes.add("缸垫密封异常导致冷却系统压力异常");
            steps.add("停车等待降温后再检查，禁止热车直接打开水箱盖");
            steps.add("检查冷却液液位、泄漏点、电子风扇和水箱表面堵塞情况");
            steps.add("观察上下水管温差，必要时检测节温器、水泵循环和冷却系统压力");
        } else if (containsAny(normalized, "异响", "抖动", "顿挫", "动力不足", "故障灯")) {
            faultType = "发动机/传动系统运行异常";
            causes.add("点火系统、进气系统或燃油喷射异常");
            causes.add("发动机机脚、变速箱控制或传动部件异常");
            causes.add("传感器报码导致 ECU 限扭或进入保护模式");
            steps.add("读取 OBD 故障码和冻结帧，记录故障出现时的转速、车速和水温");
            steps.add("检查火花塞、点火线圈、空气滤清器、节气门和进气漏气情况");
            steps.add("路试复现异响/抖动场景，区分怠速、加速、制动和转向时是否出现");
        } else if (containsAny(normalized, "漏油", "漏液", "汽油味", "烧焦味")) {
            faultType = "油液泄漏/安全风险故障";
            causes.add("发动机油封、油底壳、变速箱或助力系统存在泄漏");
            causes.add("燃油管路、冷却管路或制动管路密封异常");
            causes.add("油液滴落到排气高温部件产生异味");
            steps.add("优先确认泄漏液体颜色和位置，存在汽油味或大量漏液时停止行驶");
            steps.add("举升车辆检查底盘、发动机舱和管路接头，清洁后复查渗漏源");
            steps.add("补足对应油液后做压力/路试检查，确认不再泄漏再交车");
        } else {
            causes.add("车辆存在未明确定位的综合异常，需要结合仪表提示和路试现象进一步判断");
            causes.add("常见来源包括电气连接、传感器、油液状态、磨损件或保养不到位");
            steps.add("补充车辆品牌车型、年份、里程、故障出现频率和是否亮故障灯");
            steps.add("先做基础检查：油液、轮胎、电瓶、保险丝、可见泄漏和异味");
            steps.add("读取 OBD 故障码并结合路试复现，避免仅凭单一症状更换零件");
        }

        if ("technician".equals(role)) {
            steps.add("技师侧建议保留检测数据和故障码截图，便于后续复核与维修闭环");
        } else {
            steps.add("车主侧建议尽快到店检测；若伴随刹车失灵、高温、漏油、焦糊味或无法启动，请不要继续行驶");
        }

        String suggestion = "外部AI服务当前连接不可用，已启用本地规则诊断。\n\n"
                + "## 初步判断\n" + faultType + "\n\n"
                + "## 可能原因\n" + toMarkdownList(causes) + "\n"
                + "## 建议处理\n" + toMarkdownList(steps);

        AIDiagnosisResponse response = new AIDiagnosisResponse(faultType, suggestion);
        response.setPossibleCauses(causes);
        String severity = evaluateSeverity(text + "\n" + suggestion);
        response.setSeverityLevel(severity);
        Integer[] costRange = estimateCostBySeverity(severity);
        Integer[] hourRange = estimateHoursBySeverity(severity);
        response.setEstimatedCostMin(costRange[0]);
        response.setEstimatedCostMax(costRange[1]);
        response.setEstimatedHoursMin(hourRange[0]);
        response.setEstimatedHoursMax(hourRange[1]);
        return response;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String toMarkdownList(java.util.List<String> items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            builder.append("- ").append(item).append("\n");
        }
        return builder.toString();
    }
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "customer";
        }
        String normalized = role.trim().toLowerCase();
        return "admin".equals(normalized) ? "technician" : normalized;
    }

    private boolean isTechnicianRole(String role) {
        return "technician".equals(role);
    }

    private void appendInventoryDecisionPath(AIDiagnosisResponse response,
                                             InventoryDiagnosisAgent.InventoryEvidence inventoryEvidence,
                                             String normalizedRole) {
        if (response == null || !isTechnicianRole(normalizedRole) || inventoryEvidence == null) {
            return;
        }

        java.util.List<String> decisionPath = new java.util.ArrayList<>(response.getDecisionPath());
        decisionPath.add(inventoryEvidence.summary());
        response.setDecisionPath(decisionPath);
    }

    private void appendHistoryCaseDecisionPath(AIDiagnosisResponse response,
                                               HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence,
                                               String normalizedRole) {
        if (response == null || !isTechnicianRole(normalizedRole) || historyCaseEvidence == null) {
            return;
        }

        java.util.List<String> decisionPath = new java.util.ArrayList<>(response.getDecisionPath());
        decisionPath.add(historyCaseEvidence.summary());
        response.setDecisionPath(decisionPath);
    }

    private void appendPrivacyDecisionPath(AIDiagnosisResponse response,
                                           PrivacyMaskingService.MaskingResult maskingResult,
                                           String normalizedRole) {
        if (response == null || !isTechnicianRole(normalizedRole)) {
            return;
        }

        java.util.List<String> decisionPath = new java.util.ArrayList<>(response.getDecisionPath());
        if (maskingResult.changed()) {
            decisionPath.add("隐私脱敏: 已执行, VIN=" + maskingResult.vinCount()
                    + ", 车牌=" + maskingResult.licensePlateCount());
        } else {
            decisionPath.add("隐私脱敏: 未发现车牌/VIN");
        }
        response.setDecisionPath(decisionPath);
    }

    protected String callOpenAIAPI(String prompt, List<String> imageDataUrls, String audioDataUrl, String traceId, String stage) throws IOException {
        Object content;
        boolean hasImages = imageDataUrls != null && !imageDataUrls.isEmpty();
        boolean hasAudio = audioDataUrl != null && !audioDataUrl.isBlank();
        
        if (hasImages || hasAudio) {
            List<java.util.Map<String, Object>> contentList = new java.util.ArrayList<>();
            contentList.add(java.util.Map.of("type", "text", "text", prompt));
            
            if (hasImages) {
                for (String url : imageDataUrls) {
                    contentList.add(java.util.Map.of("type", "image_url", "image_url", java.util.Map.of("url", url)));
                }
            }
            
            if (hasAudio) {
                contentList.add(java.util.Map.of("type", "input_audio", "input_audio", java.util.Map.of("data", audioDataUrl, "format", "wav")));
            }
            
            content = contentList;
        } else {
            content = prompt;
        }

        String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                "model", model,
                "messages", List.of(java.util.Map.of(
                        "role", "user",
                        "content", content
                )),
                "temperature", 0.7
        ));

        Request request = new Request.Builder()
                .url(resolveChatCompletionsUrl())
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("API返回空响应体");
            }
            String responseBody = body.string();

            if (!response.isSuccessful()) {
                throw new IOException("API调用失败，状态码: " + response.code() + ", 响应: " + responseBody);
            }

            logger.info("[AI-CONSILIUM][{}][{}] API成功, status={}, responseLength={}",
                    traceId,
                    stage,
                    response.code(),
                    responseBody != null ? responseBody.length() : 0);

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IOException("API返回缺少choices内容");
            }
            return choices.get(0).path("message").path("content").asText();
        }
    }

    private String resolveChatCompletionsUrl() throws IOException {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IOException("AI diagnosis API base URL is not configured");
        }

        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        return normalized + "/v1/chat/completions";
    }

    private AIDiagnosisResponse parseResponse(String responseText, String problemDescription) {
        String normalizedResponse = responseText == null ? "" : responseText.trim();
        String faultType = "";
        String suggestion = "";
        java.util.List<String> possibleCauses = new java.util.ArrayList<>();

        ParsedDiagnosis structured = parseStructuredResponse(normalizedResponse);
        if (structured != null) {
            faultType = structured.faultType;
            suggestion = structured.suggestion;
            possibleCauses.addAll(structured.possibleCauses);
        }

        if (faultType.isBlank()) {
            faultType = extractLabeledSection(
                    normalizedResponse,
                    "(故障类型|故障判断|初步判断|综合结论)",
                    "(建议|维修建议|诊断建议|处理建议|排查建议|安全提示|风险提示)"
            );
        }
        if (suggestion.isBlank()) {
            suggestion = extractLabeledSection(
                    normalizedResponse,
                    "(建议|维修建议|诊断建议|处理建议|排查建议)",
                    "(故障类型|故障判断|初步判断|综合结论|安全提示|风险提示)"
            );
        }

        String[] lines = normalizedResponse.split("\n");
        for (String line : lines) {
            line = line.trim();
            String plainLine = line.replaceFirst("^#+\\s*", "");
            if (faultType.isBlank() && (plainLine.startsWith("故障类型：") || plainLine.startsWith("故障类型:"))) {
                faultType = plainLine.substring(5).trim();
            } else if (suggestion.isBlank() && (plainLine.startsWith("建议：") || plainLine.startsWith("建议:"))) {
                suggestion = plainLine.substring(3).trim();
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                String cause = line.substring(2).trim();
                if (!cause.isEmpty() && possibleCauses.size() < 5) {
                    possibleCauses.add(cause);
                }
            }
        }

        if (suggestion.isBlank()) {
            suggestion = buildSuggestionFallback(normalizedResponse, faultType);
        }

        faultType = sanitizeDiagnosisField(faultType, "故障类型");
        suggestion = sanitizeDiagnosisField(suggestion, "建议");

        if (faultType.isEmpty() && suggestion.isBlank()) {
            suggestion = normalizedResponse;
            faultType = "综合诊断";
        }

        String severity = evaluateSeverity(problemDescription + "\n" + normalizedResponse);
        Integer[] costRange = estimateCostBySeverity(severity);
        Integer[] hourRange = estimateHoursBySeverity(severity);

        AIDiagnosisResponse response = new AIDiagnosisResponse(faultType, suggestion);
        response.setSeverityLevel(severity);
        response.setPossibleCauses(possibleCauses);
        response.setEstimatedCostMin(costRange[0]);
        response.setEstimatedCostMax(costRange[1]);
        response.setEstimatedHoursMin(hourRange[0]);
        response.setEstimatedHoursMax(hourRange[1]);
        if (structured != null && structured.confidence() != null) {
            response.setConfidence(structured.confidence());
        }

        return response;
    }

    private ParsedDiagnosis parseStructuredResponse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String jsonCandidate = text.trim();
        if (jsonCandidate.startsWith("```")) {
            jsonCandidate = jsonCandidate
                    .replaceFirst("(?is)^```(?:json)?\\s*", "")
                    .replaceFirst("(?is)\\s*```$", "")
                    .trim();
        }

        int start = jsonCandidate.indexOf('{');
        int end = jsonCandidate.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }

        jsonCandidate = jsonCandidate.substring(start, end + 1);

        try {
            JsonNode root = objectMapper.readTree(jsonCandidate);
            String faultType = firstText(root, "faultType", "fault_type", "diagnosis", "故障类型");
            String suggestion = firstText(root, "suggestion", "repairSuggestion", "repair_suggestion", "诊断建议", "维修建议", "建议");
            Double confidence = firstDouble(root, "confidence", "score");
            java.util.List<String> causes = new java.util.ArrayList<>();

            JsonNode causeNode = firstNode(root, "possibleCauses", "possible_causes", "causes", "可能原因");
            if (causeNode != null && causeNode.isArray()) {
                for (JsonNode item : causeNode) {
                    String cause = item.asText("").trim();
                    if (!cause.isBlank() && causes.size() < 5) {
                        causes.add(cause);
                    }
                }
            }

            faultType = sanitizeDiagnosisField(faultType, "故障类型");
            suggestion = sanitizeDiagnosisField(suggestion, "建议");
            if (faultType.isBlank() && suggestion.isBlank() && causes.isEmpty()) {
                return null;
            }

            return new ParsedDiagnosis(faultType, suggestion, causes, confidence);
        } catch (Exception ex) {
            return null;
        }
    }

    private JsonNode firstNode(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (!node.isMissingNode() && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private String firstText(JsonNode root, String... fieldNames) {
        JsonNode node = firstNode(root, fieldNames);
        return node == null ? "" : node.asText("").trim();
    }

    private Double firstDouble(JsonNode root, String... fieldNames) {
        JsonNode node = firstNode(root, fieldNames);
        if (node == null || !node.isNumber()) {
            return null;
        }
        double value = node.asDouble();
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String extractLabeledSection(String text, String startLabels, String stopLabels) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String pattern = "(?is)(?:^|\\R)\\s*(?:#{1,6}\\s*)?(?:" + startLabels + ")\\s*[:：]?\\s*(.*?)"
                + "(?=\\R\\s*(?:#{1,6}\\s*)?(?:" + stopLabels + ")\\s*[:：]?|\\z)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(text);
        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1)
                .replaceFirst("^\\s*[\\[【]", "")
                .replaceFirst("[\\]】]\\s*$", "")
                .trim();
    }

    private String sanitizeDiagnosisField(String value, String label) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim()
                .replaceFirst("^\\s*[\\[【]", "")
                .replaceFirst("[\\]】]\\s*$", "")
                .replaceFirst("(?is)^\\s*" + label + "\\s*[:：]?\\s*", "")
                .trim();

        if (cleaned.equals(label)
                || cleaned.equals("}")
                || cleaned.equals("{")
                || cleaned.matches("(?is)^\\s*(具体|详细|分点|1-3个|用一句话).*")
                || cleaned.matches("(?is)^\\s*字段.*")) {
            return "";
        }

        return cleaned;
    }

    private String buildSuggestionFallback(String responseText, String faultType) {
        if (responseText == null || responseText.isBlank()) {
            return "";
        }

        String trimmed = responseText.trim();
        if (faultType != null && !faultType.isBlank()) {
            String remainder = trimmed.replace(faultType, "").trim();
            remainder = remainder
                    .replaceFirst("(?is)^\\s*(?:#{1,6}\\s*)?故障类型\\s*[:：]?\\s*", "")
                    .replaceFirst("^\\s*[\\[【]\\s*", "")
                    .replaceFirst("^\\s*[\\]】]\\s*", "")
                    .trim();
            if (!remainder.isBlank()) {
                return remainder;
            }
        }

        return trimmed;
    }

    private static class ParsedDiagnosis {
        private final String faultType;
        private final String suggestion;
        private final java.util.List<String> possibleCauses;
        private final Double confidence;

        private ParsedDiagnosis(String faultType, String suggestion, java.util.List<String> possibleCauses, Double confidence) {
            this.faultType = faultType == null ? "" : faultType;
            this.suggestion = suggestion == null ? "" : suggestion;
            this.possibleCauses = possibleCauses == null ? java.util.List.of() : possibleCauses;
            this.confidence = confidence;
        }

        private Double confidence() {
            return confidence;
        }
    }

    private String evaluateSeverity(String text) {
        String normalized = text == null ? "" : text.toLowerCase();
        if (normalized.contains("无法启动") || normalized.contains("刹车失灵") || normalized.contains("高温") || normalized.contains("漏油")) {
            return "CRITICAL";
        }
        if (normalized.contains("异响") || normalized.contains("抖动") || normalized.contains("故障灯") || normalized.contains("动力不足")) {
            return "HIGH";
        }
        if (normalized.contains("油耗") || normalized.contains("偶发") || normalized.contains("轻微")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Integer[] estimateCostBySeverity(String severity) {
        return switch (severity) {
            case "CRITICAL" -> new Integer[]{2000, 8000};
            case "HIGH" -> new Integer[]{800, 3000};
            case "MEDIUM" -> new Integer[]{300, 1500};
            default -> new Integer[]{100, 600};
        };
    }

    private Integer[] estimateHoursBySeverity(String severity) {
        return switch (severity) {
            case "CRITICAL" -> new Integer[]{6, 16};
            case "HIGH" -> new Integer[]{3, 8};
            case "MEDIUM" -> new Integer[]{2, 5};
            default -> new Integer[]{1, 3};
        };
    }
}

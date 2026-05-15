package org.com.repair.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.DTO.MaterialResponse;
import org.com.repair.entity.InventoryAlertNotification;
import org.com.repair.entity.RepairOrder;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.service.TechnicianService.TechnicianFatigueSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIDiagnosisService {

    private static final Logger logger = LoggerFactory.getLogger(AIDiagnosisService.class);
    private static final int MAX_IMAGE_COUNT = 3;
    private static final int MAX_IMAGE_DATA_URL_LENGTH = 7_000_000;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final double BASE_BYPASS_CONFIDENCE = 0.9;
    private static final double SAFETY_NET_THRESHOLD = 0.85;
    private static final String PRE_DIAG_PROMPT = "你是一个预诊客服。请从车主的描述中提取关键信息，严格输出JSON格式的故障特征表，包含：核心症状、疑似部位、触发条件。不要给出最终结论。";
    private static final String MAIN_AGENT_PROMPT = "你是主治维修工程师。请根据以下结构化特征表，给出初步排查路径和概率最高的三个故障点。";
    private static final String RED_TEAM_PROMPT = "你是专挑刺的QA工程师（红队）。请仔细审视主治工程师的结论，指出他可能忽略的低概率/高风险长尾故障，或者排查逻辑中的漏洞。";
    private static final String ARBITRATOR_PROMPT = "你是总控车间主任。请综合【主治诊断】和【红队质疑】，输出最终的综合问诊及排查指导报告。格式需清晰易读。";
    private static final String SEMANTIC_AGENT_PROMPT = """
            你是多模态语义Agent，请将车主非标准语音/口语化描述归一为标准维修语义。
            输出要求：
            1) 用条目列出核心症状、疑似系统、触发条件。
            2) 给出1-3个可能的故障候选（可带概率描述）。
            3) 如有图片，请结合观察给出保守判断。
            """;
    private static final String HISTORY_AGENT_PROMPT = """
            你是历史知识Agent，请基于给定的维修历史摘要，提炼常见故障类型与排查建议。
            输出要求：
            - 先总结高频故障部位
            - 再给出可复用的排查顺序
            """;
    private static final Map<String, List<String>> FAULT_CATEGORY_KEYWORDS = Map.ofEntries(
            Map.entry("启动系统", List.of("无法启动", "打不着火", "启动困难", "供电系统")),
            Map.entry("制动系统", List.of("刹车", "制动", "刹车油", "制动液")),
            Map.entry("发动机", List.of("发动机", "怠速", "抖动", "异响")),
            Map.entry("变速箱/传动", List.of("变速箱", "顿挫", "打滑", "离合")),
            Map.entry("空调系统", List.of("空调", "不制冷", "异味", "不出风")),
            Map.entry("轮胎/行驶安全", List.of("轮胎", "胎压", "爆胎", "漏气")),
            Map.entry("电气安全", List.of("冒烟", "烧焦", "焦糊", "短路", "线路"))
    );
    private static final Pattern VIN_PATTERN = Pattern.compile("\\b[A-HJ-NPR-Z0-9]{17}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLATE_PATTERN = Pattern.compile(
            "[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼]"
                    + "[A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳]?",
            Pattern.CASE_INSENSITIVE);

    @Value("${ai.diagnosis.api.key}")
    private String apiKey;

    @Value("${ai.diagnosis.api.base-url}")
    private String baseUrl;

    @Value("${ai.diagnosis.api.model}")
    private String model;

    private final GamificationService gamificationService;
    private final TechnicianService technicianService;
    private final MaterialService materialService;
    private final RepairOrderRepository repairOrderRepository;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public AIDiagnosisService(GamificationService gamificationService,
                              TechnicianService technicianService,
                              MaterialService materialService,
                              RepairOrderRepository repairOrderRepository) {
        this.gamificationService = gamificationService;
        this.technicianService = technicianService;
        this.materialService = materialService;
        this.repairOrderRepository = repairOrderRepository;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription, String role) {
        return diagnoseFault(problemDescription, role, null, List.of());
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription, String role, Long technicianId) {
        return diagnoseFault(problemDescription, role, technicianId, List.of());
    }

    public AIDiagnosisResponse diagnoseFault(String problemDescription,
                                             String role,
                                             Long technicianId,
                                             List<String> imageDataUrls) {
        String traceId = UUID.randomUUID().toString();
        String normalizedRole = normalizeRole(role);
        String normalizedProblemDescription = normalizeProblemDescription(problemDescription);
        List<String> normalizedImageDataUrls = normalizeImageDataUrls(imageDataUrls);

        logger.info("[AI-CONSILIUM][{}] 会诊开始, role={}, technicianId={}, imageCount={}",
                traceId,
                normalizedRole,
                technicianId,
                normalizedImageDataUrls.size());
        try {
            AIDiagnosisResponse response = runHybridDiagnosis(
                    normalizedProblemDescription,
                    normalizedRole,
                    technicianId,
                    normalizedImageDataUrls,
                    traceId);
            logger.info("[AI-CONSILIUM][{}] 混合诊断完成", traceId);
            return response;
        } catch (Exception e) {
            logger.warn("[AI-CONSILIUM][{}] 混合诊断失败，回退专家会诊: {}", traceId, e.getMessage(), e);
            try {
                AIDiagnosisResponse response = expertConsilium(
                        normalizedProblemDescription,
                        normalizedRole,
                        technicianId,
                        normalizedImageDataUrls,
                        traceId);
                logger.info("[AI-CONSILIUM][{}] 专家会诊回退完成", traceId);
                return response;
            } catch (Exception expertException) {
                logger.warn("[AI-CONSILIUM][{}] 专家会诊失败，回退单模型诊断: {}", traceId, expertException.getMessage(), expertException);
            }
            try {
                String fallbackPrompt = buildPrompt(normalizedProblemDescription, normalizedRole);
                String fallbackText = callOpenAIAPI(fallbackPrompt, normalizedImageDataUrls, traceId, "FALLBACK_SINGLE");
                AIDiagnosisResponse fallbackResponse = parseResponse(fallbackText, normalizedProblemDescription);
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

    private AIDiagnosisResponse runHybridDiagnosis(String problemDescription,
                                                   String role,
                                                   Long technicianId,
                                                   List<String> imageDataUrls,
                                                   String traceId) throws IOException {
        HybridDiagnosisContext context = new HybridDiagnosisContext(
                truncateDescription(problemDescription),
                role,
                technicianId,
                imageDataUrls,
                traceId);

        PrivacyReport privacyReport = runPrivacyFusion(context);
        RulePreprocessResult ruleResult = runRulePreprocessing(context);
        MultiAgentResult agentResult = ruleResult.isBypass()
                ? MultiAgentResult.empty()
                : runMultiAgentInference(context, privacyReport, ruleResult);
        FusionResult fusionResult = runDecisionFusion(context, ruleResult, agentResult, privacyReport);
        SafetyNetResult safetyNetResult = runSafetyNet(fusionResult);

        return buildHybridResponse(context, ruleResult, agentResult, fusionResult, safetyNetResult);
    }

    private AIDiagnosisResponse expertConsilium(String problemDescription,
                                                String role,
                                                Long technicianId,
                                                List<String> imageDataUrls,
                                                String traceId) throws IOException {
        List<String> ecoTips = gamificationService.getEcoWaitingTips();
        String ecoTipContext = ecoTips.isEmpty() ? "" : ("\n\n绿色导向参考：" + ecoTips.get(0));
        double fatigueLevel = resolveTechnicianFatigueLevel(technicianId, traceId);

        logger.info("[AI-CONSILIUM][{}] 融合上下文: ecoTips={}, fatigueLevel={}", traceId, ecoTips.size(), fatigueLevel);

        String preDiagInput = PRE_DIAG_PROMPT + "\n\n车主描述：" + problemDescription + ecoTipContext;
        String structuredFeatures = runAgentStep("PRE_DIAG", preDiagInput, imageDataUrls, traceId);

        String mainAgentInput = MAIN_AGENT_PROMPT +
                "\n\n角色上下文：" + role +
                "\n\n结构化特征表：\n" + structuredFeatures;
        String initialDiagnosis = runAgentStep("MAIN_AGENT", mainAgentInput, List.of(), traceId);

        String redTeamInput = RED_TEAM_PROMPT +
                "\n\n结构化特征表：\n" + structuredFeatures +
                "\n\n主治诊断：\n" + initialDiagnosis;
        String critique = runAgentStep("RED_TEAM", redTeamInput, List.of(), traceId);

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
        String finalReport = runAgentStep("ARBITRATOR", arbitratorInput, List.of(), traceId);

        return parseResponse(finalReport, problemDescription);
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
            logger.warn("[AI-CONSILIUM][{}] 技师疲劳度估算失败，回退默认值: {}", traceId, ex.getMessage(), ex);
            return defaultFatigueLevel;
        }
    }

    private RulePreprocessResult runRulePreprocessing(HybridDiagnosisContext context) {
        String description = context.problemDescription();
        List<RulePattern> patterns = List.of(
                new RulePattern("启动困难", Pattern.compile("无法启动|打不着火|启动困难"), "启动系统故障",
                        "检查电瓶、电源线路与点火系统，必要时读取启动故障码。", 0.88, 2.5),
                new RulePattern("刹车异常", Pattern.compile("刹车.*(失灵|变软|偏软|踩不动|异响)"), "制动系统异常",
                        "检查刹车油液位、真空助力和刹车片磨损情况。", 0.92, 3.0),
                new RulePattern("发动机异响", Pattern.compile("发动机.*(异响|抖动|怠速不稳|动力不足)"), "发动机运行异常",
                        "检查点火、喷油与机脚垫，必要时进行压缩比测试。", 0.85, 4.0),
                new RulePattern("变速箱顿挫", Pattern.compile("变速箱.*(顿挫|异响|打滑)|换挡.*(顿挫|迟缓)"), "变速箱/传动异常",
                        "检查变速箱油位与油质，必要时进行离合器与阀体诊断。", 0.84, 4.5),
                new RulePattern("空调异常", Pattern.compile("空调.*(不制冷|异味|不出风|不制热)"), "空调系统异常",
                        "检查冷媒压力、压缩机与鼓风机工作状态。", 0.82, 3.5),
                new RulePattern("轮胎异常", Pattern.compile("轮胎.*(漏气|爆胎|胎压)|方向.*(跑偏|抖动)"), "轮胎/行驶安全异常",
                        "检查胎压与轮胎磨损，必要时进行动平衡与四轮定位。", 0.83, 2.0),
                new RulePattern("电气风险", Pattern.compile("冒烟|烧焦味|焦糊味|短路"), "电气/高温风险",
                        "立即断电检查线束与保险丝，排查局部过热源。", 0.9, 2.5),
                new RulePattern("电瓶亏电", Pattern.compile("电瓶.*(亏电|没电|老化)|电池.*(亏电|没电)"), "供电系统异常",
                        "检查电瓶寿命、发电机输出与静态漏电。", 0.86, 2.5)
        );

        List<RulePattern> matched = patterns.stream()
                .filter(pattern -> pattern.matches(description))
                .collect(Collectors.toList());

        RulePattern primary = matched.stream()
                .max(Comparator.comparingDouble(RulePattern::confidence))
                .orElse(null);

        double confidence = primary != null ? primary.confidence() : 0.45;
        String faultType = primary != null ? primary.faultType() : "待进一步确认";
        String suggestion = primary != null
                ? primary.suggestion()
                : "症状信息不足，建议补充故障现象、时间与触发条件。";
        Double estimatedHours = primary != null ? primary.estimatedHours() : null;

        List<String> ruleHits = matched.stream()
                .map(RulePattern::label)
                .collect(Collectors.toList());
        List<String> signals = new ArrayList<>(ruleHits);

        List<HighFrequencyFault> highFrequencyFaults = List.of(
                new HighFrequencyFault("刹车异响", List.of("刹车", "异响"), "制动系统异常",
                        "先检查刹车片与盘面磨损，确认是否需要更换。", 0.05),
                new HighFrequencyFault("发动机抖动", List.of("发动机", "抖动"), "发动机运行异常",
                        "检查点火线圈与喷油系统，排除积碳。", 0.04),
                new HighFrequencyFault("空调不冷", List.of("空调", "不制冷"), "空调系统异常",
                        "检查冷媒压力与冷凝器散热情况。", 0.04),
                new HighFrequencyFault("电瓶亏电", List.of("电瓶", "亏电"), "供电系统异常",
                        "检查电瓶寿命与发电机输出电压。", 0.04)
        );

        Optional<HighFrequencyFault> highFrequencyMatch = highFrequencyFaults.stream()
                .filter(fault -> fault.matches(description))
                .findFirst();
        if (highFrequencyMatch.isPresent()) {
            HighFrequencyFault fault = highFrequencyMatch.get();
            signals.add("高频故障库:" + fault.label());
            confidence = Math.min(0.96, confidence + fault.confidenceBoost());
            if (primary == null) {
                faultType = fault.faultType();
                suggestion = fault.suggestion();
            }
        }

        if (matched.size() > 1) {
            confidence = Math.min(0.95, confidence + 0.03 * (matched.size() - 1));
        }

        boolean bypass = confidence >= BASE_BYPASS_CONFIDENCE;
        return new RulePreprocessResult(faultType, suggestion, confidence, bypass, ruleHits, signals, estimatedHours);
    }

    private PrivacyReport runPrivacyFusion(HybridDiagnosisContext context) {
        Map<String, String> tokenMap = new LinkedHashMap<>();
        String masked = maskSensitive(context.problemDescription(), VIN_PATTERN, "VIN", tokenMap);
        masked = maskSensitive(masked, PLATE_PATTERN, "PLATE", tokenMap);
        return new PrivacyReport(masked, tokenMap);
    }

    private MultiAgentResult runMultiAgentInference(HybridDiagnosisContext context,
                                                    PrivacyReport privacyReport,
                                                    RulePreprocessResult ruleResult) throws IOException {
        List<AgentEvidence> evidences = new ArrayList<>();
        evidences.add(runSemanticAgent(context, privacyReport, ruleResult));
        evidences.add(runInventoryAgent(context, ruleResult));
        evidences.add(runHistoricalAgent(context, ruleResult));

        double avgConfidence = evidences.stream()
                .mapToDouble(AgentEvidence::confidence)
                .average()
                .orElse(0.45);
        return new MultiAgentResult(evidences, avgConfidence);
    }

    private AgentEvidence runSemanticAgent(HybridDiagnosisContext context,
                                           PrivacyReport privacyReport,
                                           RulePreprocessResult ruleResult) throws IOException {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(SEMANTIC_AGENT_PROMPT)
                .append("\n\n车主描述：").append(privacyReport.maskedDescription());
        if (!ruleResult.ruleHits().isEmpty()) {
            promptBuilder.append("\n\n规则预处理命中：")
                    .append(String.join("，", ruleResult.ruleHits()));
        }
        String response = runAgentStep("SEMANTIC_AGENT", promptBuilder.toString(), context.imageDataUrls(), context.traceId());
        double confidence = evaluateAgentConfidence(response, 0.65);
        List<String> candidates = extractFaultCandidates(response);
        return new AgentEvidence("多模态语义Agent", response, confidence, candidates, List.of());
    }

    private AgentEvidence runInventoryAgent(HybridDiagnosisContext context, RulePreprocessResult ruleResult) {
        List<String> keywords = extractKeywords(context.problemDescription());
        if (ruleResult.faultType() != null) {
            if (ruleResult.faultType().contains("制动")) {
                keywords.add("刹车");
            }
            if (ruleResult.faultType().contains("发动机")) {
                keywords.add("发动机");
            }
            if (ruleResult.faultType().contains("变速箱")) {
                keywords.add("变速箱");
            }
            if (ruleResult.faultType().contains("空调")) {
                keywords.add("空调");
            }
            if (ruleResult.faultType().contains("供电")) {
                keywords.add("电瓶");
            }
        }
        List<String> materialHints = new ArrayList<>();
        List<String> lowStockHints = new ArrayList<>();

        Map<String, List<String>> materialKeywordMap = Map.ofEntries(
                Map.entry("刹车", List.of("刹车片", "刹车油")),
                Map.entry("发动机", List.of("机油", "火花塞", "点火线圈")),
                Map.entry("变速箱", List.of("变速箱油", "离合器")),
                Map.entry("空调", List.of("冷媒", "压缩机")),
                Map.entry("电瓶", List.of("电瓶", "蓄电池")),
                Map.entry("轮胎", List.of("轮胎", "胎压"))
        );

        Set<String> candidateMaterials = new LinkedHashSet<>();
        Set<String> keywordSet = new LinkedHashSet<>(keywords);
        for (String keyword : keywordSet) {
            materialKeywordMap.forEach((key, materials) -> {
                if (keyword.contains(key)) {
                    candidateMaterials.addAll(materials);
                }
            });
        }

        for (String materialName : candidateMaterials) {
            List<MaterialResponse> materials = materialService.getMaterialsByName(materialName);
            for (MaterialResponse material : materials) {
                Integer stock = material.stockQuantity();
                Integer minimum = material.minimumStockLevel();
                materialHints.add(material.name() + "(库存:" + stock + ")");
                if (stock != null && minimum != null && stock < minimum) {
                    lowStockHints.add(material.name());
                }
            }
        }

        List<InventoryAlertNotification> alerts = materialService.getActiveInventoryAlerts();
        if (!alerts.isEmpty()) {
            lowStockHints.addAll(alerts.stream()
                    .map(InventoryAlertNotification::getMaterialName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());
        }

        String summary;
        if (materialHints.isEmpty()) {
            summary = "未检索到可关联的库存材料，建议在确认故障部位后补充库存校验。";
        } else {
            summary = "关联材料：" + String.join("，", materialHints);
            if (!lowStockHints.isEmpty()) {
                summary += "；低库存预警：" + String.join("，", lowStockHints);
            }
        }

        double confidence = materialHints.isEmpty() ? 0.45 : 0.7;
        return new AgentEvidence("库存关联Agent", summary, confidence, List.of(), materialHints);
    }

    private AgentEvidence runHistoricalAgent(HybridDiagnosisContext context, RulePreprocessResult ruleResult) {
        List<String> keywords = extractKeywords(context.problemDescription());
        if (keywords.isEmpty() && !ruleResult.signals().isEmpty()) {
            keywords = ruleResult.signals();
        }

        List<RepairOrder> orders = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            orders.addAll(repairOrderRepository.findTop5ByDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(keyword));
            if (orders.size() >= 5) {
                break;
            }
        }

        Map<Long, RepairOrder> deduped = new LinkedHashMap<>();
        for (RepairOrder order : orders) {
            if (order != null && order.getId() != null) {
                deduped.put(order.getId(), order);
            }
        }
        List<RepairOrder> recentOrders = new ArrayList<>(deduped.values());
        if (recentOrders.size() > 5) {
            recentOrders = recentOrders.subList(0, 5);
        }

        String summary;
        if (recentOrders.isEmpty()) {
            summary = "暂无相似历史工单，建议结合现场检查补充案例。";
        } else {
            List<String> orderSummaries = recentOrders.stream()
                    .map(order -> {
                        String skill = order.getRequiredSkillType() == null ? "未知工种" : order.getRequiredSkillType().name();
                        String repairType = order.getRepairType() == null ? "未知类型" : order.getRepairType();
                        String desc = order.getDescription();
                        if (desc != null && desc.length() > 60) {
                            desc = desc.substring(0, 60) + "...";
                        }
                        return "工单#" + order.getOrderNumber() + " (" + skill + "/" + repairType + "): " + desc;
                    })
                    .collect(Collectors.toList());
            summary = "历史工单参考：" + String.join("；", orderSummaries);
        }

        String refinedSummary = summary;
        if (!recentOrders.isEmpty()) {
            String prompt = HISTORY_AGENT_PROMPT + "\n\n历史摘要：\n" + summary;
            try {
                refinedSummary = runAgentStep("HISTORY_AGENT", prompt, List.of(), context.traceId());
            } catch (IOException ex) {
                logger.warn("[AI-CONSILIUM][{}][HISTORY_AGENT] 历史Agent调用失败，使用原始摘要: {}",
                        context.traceId(),
                        ex.getMessage());
            }
        }

        double confidence = recentOrders.isEmpty() ? 0.45 : (recentOrders.size() >= 3 ? 0.72 : 0.62);
        List<String> candidates = extractFaultCandidates(refinedSummary);
        return new AgentEvidence("历史知识Agent", refinedSummary, confidence, candidates, List.of());
    }

    private FusionResult runDecisionFusion(HybridDiagnosisContext context,
                                           RulePreprocessResult ruleResult,
                                           MultiAgentResult agentResult,
                                           PrivacyReport privacyReport) {
        List<String> candidates = new ArrayList<>();
        if (ruleResult.faultType() != null && !"待进一步确认".equals(ruleResult.faultType())) {
            candidates.add(ruleResult.faultType());
        }
        for (AgentEvidence evidence : agentResult.evidences()) {
            candidates.addAll(evidence.faultCandidates());
        }

        Map<String, Long> counts = candidates.stream()
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.groupingBy(item -> item, Collectors.counting()));
        String finalFault = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ruleResult.faultType());

        if (finalFault == null || finalFault.isBlank()) {
            finalFault = "待进一步确认";
        }

        double ruleWeight = agentResult.isEmpty() ? 0.75 : 0.45;
        double agentWeight = agentResult.isEmpty() ? 0.25 : 0.55;
        double confidence = ruleWeight * ruleResult.confidence() + agentWeight * agentResult.confidence();

        Optional<String> ruleCategory = resolveFaultCategory(ruleResult.faultType() + " " + context.problemDescription());
        Optional<String> agentCategory = resolveFaultCategory(agentResult.summaryText());
        boolean conflict = ruleCategory.isPresent() && agentCategory.isPresent()
                && !Objects.equals(ruleCategory.get(), agentCategory.get());
        if (conflict) {
            confidence = Math.max(0.35, confidence - 0.08);
        }

        String report = buildFusionReport(context, ruleResult, agentResult, finalFault, confidence, conflict);
        report = privacyReport.restore(report);

        List<String> decisionPath = new ArrayList<>();
        decisionPath.add(ruleResult.isBypass() ? "规则预处理层直接命中" : "规则预处理层+多智能体协同推理");
        if (conflict) {
            decisionPath.add("冲突消解：以多证据加权后取置信度更高结论");
        } else {
            decisionPath.add("迟融合策略：规则+Agent加权一致");
        }

        List<String> causes = candidates.stream()
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        return new FusionResult(finalFault, report, confidence, causes, decisionPath);
    }

    private SafetyNetResult runSafetyNet(FusionResult fusionResult) {
        boolean approved = fusionResult.confidence() >= SAFETY_NET_THRESHOLD;
        String status = approved ? "VALIDATED" : "SUSPENDED";
        StringBuilder reportBuilder = new StringBuilder(fusionResult.report());
        reportBuilder.append("\n\n---\n");
        if (approved) {
            reportBuilder.append("✅ 全局置信度监测通过：").append(String.format("%.2f", fusionResult.confidence()))
                    .append("，输出最终验证报告。");
        } else {
            reportBuilder.append("⚠️ 全局置信度仅为 ")
                    .append(String.format("%.2f", fusionResult.confidence()))
                    .append("，已自动挂起并进入人工复核流程。");
        }
        return new SafetyNetResult(approved, fusionResult.confidence(), status, reportBuilder.toString());
    }

    private AIDiagnosisResponse buildHybridResponse(HybridDiagnosisContext context,
                                                    RulePreprocessResult ruleResult,
                                                    MultiAgentResult agentResult,
                                                    FusionResult fusionResult,
                                                    SafetyNetResult safetyNetResult) {
        String faultType = safetyNetResult.approved() ? fusionResult.faultType() : "待人工复核";
        AIDiagnosisResponse response = new AIDiagnosisResponse(faultType, safetyNetResult.report());

        String severity = evaluateSeverity(context.problemDescription() + "\n" + safetyNetResult.report());
        Integer[] costRange = estimateCostBySeverity(severity);
        Integer[] hourRange = estimateHoursBySeverity(severity);

        response.setSeverityLevel(severity);
        response.setPossibleCauses(fusionResult.causes());
        response.setEstimatedCostMin(costRange[0]);
        response.setEstimatedCostMax(costRange[1]);

        if (ruleResult.estimatedHours() != null) {
            int base = Math.max(1, (int) Math.round(ruleResult.estimatedHours()));
            response.setEstimatedHoursMin(Math.max(1, base - 1));
            response.setEstimatedHoursMax(base + 2);
        } else {
            response.setEstimatedHoursMin(hourRange[0]);
            response.setEstimatedHoursMax(hourRange[1]);
        }

        response.setConfidenceScore(safetyNetResult.confidence());
        response.setWorkflowStatus(safetyNetResult.status());
        response.setRuleHits(ruleResult.ruleHits());
        response.setAgentSummaries(agentResult.evidences().stream()
                .map(AgentEvidence::summaryHeadline)
                .collect(Collectors.toList()));

        return response;
    }

    private String buildFusionReport(HybridDiagnosisContext context,
                                     RulePreprocessResult ruleResult,
                                     MultiAgentResult agentResult,
                                     String finalFault,
                                     double confidence,
                                     boolean conflict) {
        StringBuilder builder = new StringBuilder();
        builder.append("## 综合诊断报告\n\n");
        builder.append("### 规则预处理层\n");
        if (ruleResult.ruleHits().isEmpty()) {
            builder.append("- 未命中规则，进入协同推理补充诊断。\n");
        } else {
            builder.append("- 命中规则：").append(String.join("，", ruleResult.ruleHits())).append("\n");
        }
        builder.append("- 初步判断：").append(ruleResult.faultType())
                .append("（置信度 ").append(String.format("%.2f", ruleResult.confidence())).append("）\n");
        builder.append("- 初步建议：").append(ruleResult.suggestion()).append("\n\n");

        if (!agentResult.isEmpty()) {
            builder.append("### 多智能体协同推理层\n");
            for (AgentEvidence evidence : agentResult.evidences()) {
                builder.append("- **").append(evidence.name()).append("**：")
                        .append(evidence.summary()).append("\n");
            }
            builder.append("\n");
        }

        builder.append("### 决策融合层\n");
        builder.append("- 迟融合策略：规则权重45%，Agent权重55%。\n");
        builder.append("- 加权证据推理：综合置信度 ").append(String.format("%.2f", confidence)).append("。\n");
        if (conflict) {
            builder.append("- 冲突消解：规则与Agent结论不一致，已按加权证据重新排序。\n");
        }
        builder.append("\n### 综合结论\n");
        builder.append("**故障类型**：").append(finalFault).append("\n\n");
        builder.append("**排查建议**：").append(ruleResult.suggestion()).append("\n");

        return builder.toString();
    }

    private String maskSensitive(String text, Pattern pattern, String placeholderPrefix, Map<String, String> tokenMap) {
        if (text == null || text.isBlank()) {
            return text;
        }
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        int index = 1;
        while (matcher.find()) {
            String token = matcher.group();
            String placeholder = "[" + placeholderPrefix + "_" + index + "]";
            tokenMap.put(placeholder, token);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(placeholder));
            index++;
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private List<String> extractKeywords(String description) {
        if (description == null || description.isBlank()) {
            return List.of();
        }
        List<String> candidates = List.of("刹车", "发动机", "变速箱", "空调", "电瓶", "电池", "轮胎", "异响", "抖动");
        List<String> results = new ArrayList<>();
        for (String candidate : candidates) {
            if (description.contains(candidate)) {
                results.add(candidate);
            }
        }
        return results;
    }

    private List<String> extractFaultCandidates(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : FAULT_CATEGORY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    result.add(entry.getKey() + "异常");
                    break;
                }
            }
        }
        return new ArrayList<>(result);
    }

    private Optional<String> resolveFaultCategory(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        for (Map.Entry<String, List<String>> entry : FAULT_CATEGORY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

    private double evaluateAgentConfidence(String response, double defaultScore) {
        if (response == null || response.isBlank()) {
            return defaultScore;
        }
        double score = defaultScore;
        if (response.contains("不确定") || response.contains("无法判断")) {
            score -= 0.1;
        }
        if (response.length() > 200) {
            score += 0.05;
        }
        return Math.min(0.9, Math.max(0.35, score));
    }

    private String runAgentStep(String stage, String prompt, List<String> imageDataUrls, String traceId) throws IOException {
        logger.info("[AI-CONSILIUM][{}][{}] 调用开始, promptLength={}", traceId, stage, prompt != null ? prompt.length() : 0);
        String result = callOpenAIAPI(prompt, imageDataUrls, traceId, stage);
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

    private String normalizeProblemDescription(String problemDescription) {
        String normalized = problemDescription == null ? "" : problemDescription.trim();
        if (normalized.isBlank()) {
            return "（用户未提供文字描述，仅上传了故障图片）";
        }
        return truncateDescription(normalized);
    }

    private String truncateDescription(String description) {
        if (description == null) {
            return "";
        }
        String trimmed = description.trim();
        if (trimmed.length() <= MAX_DESCRIPTION_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_DESCRIPTION_LENGTH) + "...";
    }

    private List<String> normalizeImageDataUrls(List<String> imageDataUrls) {
        if (imageDataUrls == null || imageDataUrls.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String imageDataUrl : imageDataUrls) {
            if (imageDataUrl == null) {
                continue;
            }

            String trimmed = imageDataUrl.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            String normalizedDataUrl = trimmed.startsWith("data:image/")
                    ? trimmed
                    : "data:image/jpeg;base64," + trimmed;

            if (!normalizedDataUrl.startsWith("data:image/") || !normalizedDataUrl.contains(";base64,")) {
                throw new IllegalArgumentException("图片格式无效，请上传 PNG/JPG/WebP 图片");
            }

            if (normalizedDataUrl.length() > MAX_IMAGE_DATA_URL_LENGTH) {
                throw new IllegalArgumentException("上传图片过大，请压缩后重试（单张不超过约5MB）");
            }

            normalized.add(normalizedDataUrl);
            if (normalized.size() >= MAX_IMAGE_COUNT) {
                break;
            }
        }

        return normalized;
    }

    private String callOpenAIAPI(String prompt, List<String> imageDataUrls, String traceId, String stage) throws IOException {
        List<String> normalizedImageDataUrls = normalizeImageDataUrls(imageDataUrls);

        try {
            return doCallOpenAIAPI(prompt, normalizedImageDataUrls, traceId, stage);
        } catch (IOException imageModeException) {
            if (normalizedImageDataUrls.isEmpty()) {
                throw imageModeException;
            }

            logger.warn("[AI-CONSILIUM][{}][{}] 多模态调用失败，回退文本模式: {}",
                    traceId,
                    stage,
                    imageModeException.getMessage());

            String textOnlyPrompt = prompt + "\n\n补充说明：用户还上传了"
                    + normalizedImageDataUrls.size()
                    + "张故障图片，请在缺少图像细节时给出保守诊断建议。";

            return doCallOpenAIAPI(textOnlyPrompt, List.of(), traceId, stage + "_TEXT_ONLY_FALLBACK");
        }
    }

    private String doCallOpenAIAPI(String prompt, List<String> imageDataUrls, String traceId, String stage) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.7);

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");

        if (imageDataUrls == null || imageDataUrls.isEmpty()) {
            userMessage.put("content", prompt);
        } else {
            List<Object> contentList = new ArrayList<>();

            Map<String, Object> textBlock = new LinkedHashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", prompt);
            contentList.add(textBlock);

            for (String imageDataUrl : imageDataUrls) {
                Map<String, Object> imageBlock = new LinkedHashMap<>();
                imageBlock.put("type", "image_url");
                imageBlock.put("image_url", Map.of("url", imageDataUrl));
                contentList.add(imageBlock);
            }

            userMessage.put("content", contentList);
        }

        payload.put("messages", List.of(userMessage));
        String requestBody = objectMapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
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

    private AIDiagnosisResponse parseResponse(String responseText, String problemDescription) {
        // 解析AI返回的文本
        String faultType = "";
        String suggestion = "";
        java.util.List<String> possibleCauses = new java.util.ArrayList<>();

        String[] lines = responseText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("故障类型：") || line.startsWith("故障类型:")) {
                faultType = line.substring(5).trim();
            } else if (line.startsWith("建议：") || line.startsWith("建议:")) {
                suggestion = line.substring(3).trim();
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                String cause = line.substring(2).trim();
                if (!cause.isEmpty() && possibleCauses.size() < 5) {
                    possibleCauses.add(cause);
                }
            }
        }

        // 如果没有按格式返回，使用整个响应作为建议
        if (faultType.isEmpty() && suggestion.isEmpty()) {
            suggestion = responseText;
            faultType = "综合诊断";
        }

        String severity = evaluateSeverity(problemDescription + "\n" + responseText);
        Integer[] costRange = estimateCostBySeverity(severity);
        Integer[] hourRange = estimateHoursBySeverity(severity);

        AIDiagnosisResponse response = new AIDiagnosisResponse(faultType, suggestion);
        response.setSeverityLevel(severity);
        response.setPossibleCauses(possibleCauses);
        response.setEstimatedCostMin(costRange[0]);
        response.setEstimatedCostMax(costRange[1]);
        response.setEstimatedHoursMin(hourRange[0]);
        response.setEstimatedHoursMax(hourRange[1]);

        return response;
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

    private static class HybridDiagnosisContext {
        private final String problemDescription;
        private final String role;
        private final Long technicianId;
        private final List<String> imageDataUrls;
        private final String traceId;

        private HybridDiagnosisContext(String problemDescription,
                                       String role,
                                       Long technicianId,
                                       List<String> imageDataUrls,
                                       String traceId) {
            this.problemDescription = problemDescription;
            this.role = role;
            this.technicianId = technicianId;
            this.imageDataUrls = imageDataUrls == null ? List.of() : List.copyOf(imageDataUrls);
            this.traceId = traceId;
        }

        public String problemDescription() {
            return problemDescription;
        }

        public String role() {
            return role;
        }

        public Long technicianId() {
            return technicianId;
        }

        public List<String> imageDataUrls() {
            return imageDataUrls;
        }

        public String traceId() {
            return traceId;
        }
    }

    private static class RulePreprocessResult {
        private final String faultType;
        private final String suggestion;
        private final double confidence;
        private final boolean bypass;
        private final List<String> ruleHits;
        private final List<String> signals;
        private final Double estimatedHours;

        private RulePreprocessResult(String faultType,
                                     String suggestion,
                                     double confidence,
                                     boolean bypass,
                                     List<String> ruleHits,
                                     List<String> signals,
                                     Double estimatedHours) {
            this.faultType = faultType;
            this.suggestion = suggestion;
            this.confidence = confidence;
            this.bypass = bypass;
            this.ruleHits = ruleHits == null ? List.of() : List.copyOf(ruleHits);
            this.signals = signals == null ? List.of() : List.copyOf(signals);
            this.estimatedHours = estimatedHours;
        }

        public String faultType() {
            return faultType;
        }

        public String suggestion() {
            return suggestion;
        }

        public double confidence() {
            return confidence;
        }

        public boolean isBypass() {
            return bypass;
        }

        public List<String> ruleHits() {
            return ruleHits;
        }

        public List<String> signals() {
            return signals;
        }

        public Double estimatedHours() {
            return estimatedHours;
        }
    }

    private static class AgentEvidence {
        private final String name;
        private final String summary;
        private final double confidence;
        private final List<String> faultCandidates;
        private final List<String> materialHints;

        private AgentEvidence(String name,
                              String summary,
                              double confidence,
                              List<String> faultCandidates,
                              List<String> materialHints) {
            this.name = name;
            this.summary = summary;
            this.confidence = confidence;
            this.faultCandidates = faultCandidates == null ? List.of() : List.copyOf(faultCandidates);
            this.materialHints = materialHints == null ? List.of() : List.copyOf(materialHints);
        }

        public String name() {
            return name;
        }

        public String summary() {
            return summary;
        }

        public double confidence() {
            return confidence;
        }

        public List<String> faultCandidates() {
            return faultCandidates;
        }

        public List<String> materialHints() {
            return materialHints;
        }

        public String summaryHeadline() {
            if (summary == null || summary.isBlank()) {
                return name + "：无可用摘要";
            }
            String trimmed = summary.replaceAll("\\s+", " ");
            if (trimmed.length() > 120) {
                trimmed = trimmed.substring(0, 120) + "...";
            }
            return name + "：" + trimmed;
        }
    }

    private static class MultiAgentResult {
        private final List<AgentEvidence> evidences;
        private final double confidence;

        private MultiAgentResult(List<AgentEvidence> evidences, double confidence) {
            this.evidences = evidences == null ? List.of() : List.copyOf(evidences);
            this.confidence = confidence;
        }

        public static MultiAgentResult empty() {
            return new MultiAgentResult(List.of(), 0.45);
        }

        public List<AgentEvidence> evidences() {
            return evidences;
        }

        public double confidence() {
            return confidence;
        }

        public boolean isEmpty() {
            return evidences.isEmpty();
        }

        public String summaryText() {
            return evidences.stream()
                    .map(AgentEvidence::summary)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
        }
    }

    private static class FusionResult {
        private final String faultType;
        private final String report;
        private final double confidence;
        private final List<String> causes;
        private final List<String> decisionPath;

        private FusionResult(String faultType,
                             String report,
                             double confidence,
                             List<String> causes,
                             List<String> decisionPath) {
            this.faultType = faultType;
            this.report = report;
            this.confidence = confidence;
            this.causes = causes == null ? List.of() : List.copyOf(causes);
            this.decisionPath = decisionPath == null ? List.of() : List.copyOf(decisionPath);
        }

        public String faultType() {
            return faultType;
        }

        public String report() {
            return report;
        }

        public double confidence() {
            return confidence;
        }

        public List<String> causes() {
            return causes;
        }

        public List<String> decisionPath() {
            return decisionPath;
        }
    }

    private static class SafetyNetResult {
        private final boolean approved;
        private final double confidence;
        private final String status;
        private final String report;

        private SafetyNetResult(boolean approved, double confidence, String status, String report) {
            this.approved = approved;
            this.confidence = confidence;
            this.status = status;
            this.report = report;
        }

        public boolean approved() {
            return approved;
        }

        public double confidence() {
            return confidence;
        }

        public String status() {
            return status;
        }

        public String report() {
            return report;
        }
    }

    private static class PrivacyReport {
        private final String maskedDescription;
        private final Map<String, String> tokenMap;

        private PrivacyReport(String maskedDescription, Map<String, String> tokenMap) {
            this.maskedDescription = maskedDescription == null ? "" : maskedDescription;
            this.tokenMap = tokenMap == null ? Map.of() : Map.copyOf(tokenMap);
        }

        public String maskedDescription() {
            return maskedDescription;
        }

        public String restore(String text) {
            if (text == null || tokenMap.isEmpty()) {
                return text;
            }
            String restored = text;
            for (Map.Entry<String, String> entry : tokenMap.entrySet()) {
                restored = restored.replace(entry.getKey(), entry.getValue());
            }
            return restored;
        }
    }

    private static class RulePattern {
        private final String label;
        private final Pattern pattern;
        private final String faultType;
        private final String suggestion;
        private final double confidence;
        private final Double estimatedHours;

        private RulePattern(String label,
                            Pattern pattern,
                            String faultType,
                            String suggestion,
                            double confidence,
                            Double estimatedHours) {
            this.label = label;
            this.pattern = pattern;
            this.faultType = faultType;
            this.suggestion = suggestion;
            this.confidence = confidence;
            this.estimatedHours = estimatedHours;
        }

        public boolean matches(String description) {
            if (description == null || description.isBlank()) {
                return false;
            }
            return pattern.matcher(description).find();
        }

        public String label() {
            return label;
        }

        public String faultType() {
            return faultType;
        }

        public String suggestion() {
            return suggestion;
        }

        public double confidence() {
            return confidence;
        }

        public Double estimatedHours() {
            return estimatedHours;
        }
    }

    private static class HighFrequencyFault {
        private final String label;
        private final List<String> keywords;
        private final String faultType;
        private final String suggestion;
        private final double confidenceBoost;

        private HighFrequencyFault(String label,
                                   List<String> keywords,
                                   String faultType,
                                   String suggestion,
                                   double confidenceBoost) {
            this.label = label;
            this.keywords = keywords == null ? List.of() : List.copyOf(keywords);
            this.faultType = faultType;
            this.suggestion = suggestion;
            this.confidenceBoost = confidenceBoost;
        }

        public boolean matches(String description) {
            if (description == null || description.isBlank()) {
                return false;
            }
            return keywords.stream().allMatch(description::contains);
        }

        public String label() {
            return label;
        }

        public String faultType() {
            return faultType;
        }

        public String suggestion() {
            return suggestion;
        }

        public double confidenceBoost() {
            return confidenceBoost;
        }
    }
}

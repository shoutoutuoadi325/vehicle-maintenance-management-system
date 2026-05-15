package org.com.repair.architecture.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HybridDiagnosisShowcase {

    private static final double RULE_BYPASS_THRESHOLD = 0.9;
    private static final double SAFETY_NET_THRESHOLD = 0.85;
    private static final double RULE_WEIGHT_WITH_AGENTS = 0.45;
    private static final double AGENT_WEIGHT_WITH_RULES = 0.55;
    private static final double RULE_WEIGHT_ALONE = 0.75;
    private static final double AGENT_WEIGHT_FALLBACK = 0.25;

    private final RulePreprocessor rulePreprocessor = new RulePreprocessor();
    private final PrivacyFilter privacyFilter = new PrivacyFilter();
    private final SemanticAgent semanticAgent;
    private final InventoryAgent inventoryAgent;
    private final HistoryAgent historyAgent;
    private final DecisionFusionEngine fusionEngine = new DecisionFusionEngine();
    private final SafetyNetGate safetyNetGate = new SafetyNetGate();

    public HybridDiagnosisShowcase(LanguageModelClient languageModelClient,
                                   InventorySignalProvider inventorySignalProvider,
                                   HistoryCaseProvider historyCaseProvider) {
        this.semanticAgent = new SemanticAgent(languageModelClient);
        this.inventoryAgent = new InventoryAgent(inventorySignalProvider);
        this.historyAgent = new HistoryAgent(historyCaseProvider);
    }

    public DiagnosisReport diagnose(AgentContext context) {
        PrivacyResult privacy = privacyFilter.mask(context.description());
        RulePreprocessResult ruleResult = rulePreprocessor.evaluate(privacy.maskedDescription());

        List<AgentEvidence> evidences = new ArrayList<>();
        if (!ruleResult.bypass()) {
            evidences.add(semanticAgent.analyze(context, privacy, ruleResult));
            evidences.add(inventoryAgent.analyze(context, ruleResult));
            evidences.add(historyAgent.analyze(context, ruleResult));
        }

        FusionDecision fusionDecision = fusionEngine.fuse(context, ruleResult, evidences);
        SafetyNetDecision safetyNetDecision = safetyNetGate.apply(fusionDecision);
        String restoredReport = privacy.restore(safetyNetDecision.report());

        return new DiagnosisReport(
                fusionDecision.faultType(),
                restoredReport,
                safetyNetDecision.confidence(),
                safetyNetDecision.status(),
                fusionDecision.decisionPath(),
                ruleResult.ruleHits(),
                evidences.stream().map(AgentEvidence::headline).toList());
    }

    public record AgentContext(String description,
                               String role,
                               List<String> imageUrls,
                               Map<String, String> metadata,
                               Instant createdAt) {
        public AgentContext {
            if (imageUrls == null) {
                imageUrls = List.of();
            }
            if (metadata == null) {
                metadata = Map.of();
            }
            if (createdAt == null) {
                createdAt = Instant.now();
            }
        }
    }

    public record DiagnosisReport(String faultType,
                                  String report,
                                  double confidence,
                                  String workflowStatus,
                                  List<String> decisionPath,
                                  List<String> ruleHits,
                                  List<String> agentSummaries) {
    }

    public record RulePreprocessResult(String faultType,
                                       String suggestion,
                                       double confidence,
                                       boolean bypass,
                                       List<String> ruleHits,
                                       List<String> signals) {
    }

    public record AgentEvidence(String agentName,
                                String summary,
                                double confidence,
                                List<String> faultCandidates,
                                List<String> riskTags) {
        public String headline() {
            String safeSummary = summary == null ? "" : summary.replaceAll("\\s+", " ");
            if (safeSummary.length() > 120) {
                safeSummary = safeSummary.substring(0, 120) + "...";
            }
            return agentName + "：" + safeSummary;
        }
    }

    public record FusionDecision(String faultType,
                                 double confidence,
                                 String report,
                                 List<String> possibleCauses,
                                 List<String> decisionPath) {
    }

    public record SafetyNetDecision(String status, double confidence, String report) {
    }

    public interface LanguageModelClient {
        String complete(String prompt, List<String> images);
    }

    public interface InventorySignalProvider {
        List<InventorySignal> fetchSignals(List<String> keywords);
    }

    public interface HistoryCaseProvider {
        List<CaseSummary> recentCases(List<String> keywords);
    }

    public record InventorySignal(String materialName, Integer stock, Integer minimum, String severity) {
    }

    public record CaseSummary(String orderNo, String summary, String category) {
    }

    public static class RulePreprocessor {
        private static final String GAP_REGEX = "[\\u4e00-\\u9fa5A-Za-z0-9]{0,8}";
        private final List<RulePattern> patterns = List.of(
                new RulePattern("启动困难", Pattern.compile("无法启动|打不着火|启动困难"), "启动系统故障",
                        "检查电瓶、电源线路与点火系统，必要时读取启动故障码。", 0.88),
                new RulePattern("刹车异常", Pattern.compile("刹车" + GAP_REGEX + "(失灵|变软|偏软|踩不动|异响)"), "制动系统异常",
                        "检查刹车油液位、真空助力和刹车片磨损情况。", 0.92),
                new RulePattern("发动机异响", Pattern.compile("发动机" + GAP_REGEX + "(异响|抖动|怠速不稳|动力不足)"), "发动机运行异常",
                        "检查点火、喷油与机脚垫，必要时进行压缩比测试。", 0.85),
                new RulePattern("变速箱顿挫", Pattern.compile("变速箱" + GAP_REGEX + "(顿挫|异响|打滑)|换挡" + GAP_REGEX + "(顿挫|迟缓)"), "变速箱/传动异常",
                        "检查变速箱油位与油质，必要时进行离合器与阀体诊断。", 0.84),
                new RulePattern("空调异常", Pattern.compile("空调" + GAP_REGEX + "(不制冷|异味|不出风|不制热)"), "空调系统异常",
                        "检查冷媒压力、压缩机与鼓风机工作状态。", 0.82),
                new RulePattern("电气风险", Pattern.compile("冒烟|烧焦味|焦糊味|短路"), "电气/高温风险",
                        "立即断电检查线束与保险丝，排查局部过热源。", 0.9)
        );

        public RulePreprocessResult evaluate(String description) {
            List<RulePattern> matched = patterns.stream()
                    .filter(pattern -> pattern.matches(description))
                    .toList();
            RulePattern primary = matched.stream()
                    .max(Comparator.comparingDouble(RulePattern::confidence))
                    .orElse(null);

            String faultType = primary == null ? "待进一步确认" : primary.faultType();
            String suggestion = primary == null ? "建议补充故障细节（时间、环境、触发条件）。" : primary.suggestion();
            double confidence = primary == null ? 0.45 : primary.confidence();

            List<String> ruleHits = matched.stream().map(RulePattern::label).toList();
            List<String> signals = new ArrayList<>(ruleHits);
            boolean bypass = confidence >= RULE_BYPASS_THRESHOLD;
            return new RulePreprocessResult(faultType, suggestion, confidence, bypass, ruleHits, signals);
        }
    }

    public static class SemanticAgent {
        private final LanguageModelClient languageModelClient;

        public SemanticAgent(LanguageModelClient languageModelClient) {
            this.languageModelClient = languageModelClient;
        }

        public AgentEvidence analyze(AgentContext context,
                                     PrivacyResult privacy,
                                     RulePreprocessResult ruleResult) {
            String prompt = """
                    你是多模态语义Agent，请将车主描述转成标准化的故障语义。
                    输出要求：核心症状、疑似系统、触发条件、候选故障点。
                    """ + "\n\n车主描述：" + privacy.maskedDescription();
            String response = languageModelClient.complete(prompt, context.imageUrls());
            double confidence = response == null || response.isBlank() ? 0.45 : 0.7;
            List<String> candidates = FaultKeywordMatcher.extractCandidates(response);
            return new AgentEvidence("多模态语义Agent", defaultIfBlank(response, "未获取到语义摘要"), confidence, candidates, List.of());
        }
    }

    public static class InventoryAgent {
        private final InventorySignalProvider inventorySignalProvider;

        public InventoryAgent(InventorySignalProvider inventorySignalProvider) {
            this.inventorySignalProvider = inventorySignalProvider;
        }

        public AgentEvidence analyze(AgentContext context, RulePreprocessResult ruleResult) {
            List<String> keywords = FaultKeywordMatcher.extractKeywords(context.description(), ruleResult.signals());
            List<InventorySignal> signals = inventorySignalProvider.fetchSignals(keywords);
            if (signals == null || signals.isEmpty()) {
                return new AgentEvidence("库存关联Agent",
                        "未发现与故障描述相关的库存预警材料。",
                        0.45,
                        List.of(),
                        List.of());
            }

            String summary = signals.stream()
                    .map(signal -> signal.materialName() + "(库存:" + signal.stock() + ")")
                    .collect(Collectors.joining("，"));
            List<String> riskTags = signals.stream()
                    .map(InventorySignal::severity)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            return new AgentEvidence("库存关联Agent",
                    "关联材料：" + summary,
                    0.7,
                    List.of(),
                    riskTags);
        }
    }

    public static class HistoryAgent {
        private final HistoryCaseProvider historyCaseProvider;

        public HistoryAgent(HistoryCaseProvider historyCaseProvider) {
            this.historyCaseProvider = historyCaseProvider;
        }

        public AgentEvidence analyze(AgentContext context, RulePreprocessResult ruleResult) {
            List<String> keywords = FaultKeywordMatcher.extractKeywords(context.description(), ruleResult.signals());
            List<CaseSummary> cases = historyCaseProvider.recentCases(keywords);
            if (cases == null || cases.isEmpty()) {
                return new AgentEvidence("历史知识Agent",
                        "暂无相似历史工单，可提示补充现场检查数据。",
                        0.45,
                        List.of(),
                        List.of());
            }
            String summary = cases.stream()
                    .map(caseSummary -> caseSummary.orderNo() + ":" + caseSummary.summary())
                    .collect(Collectors.joining("；"));
            List<String> candidates = cases.stream()
                    .map(CaseSummary::category)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            return new AgentEvidence("历史知识Agent",
                    "历史参考：" + summary,
                    0.65,
                    candidates,
                    List.of());
        }
    }

    public static class DecisionFusionEngine {
        public FusionDecision fuse(AgentContext context,
                                   RulePreprocessResult ruleResult,
                                   List<AgentEvidence> evidences) {
            List<String> candidates = new ArrayList<>();
            if (ruleResult.faultType() != null) {
                candidates.add(ruleResult.faultType());
            }
            for (AgentEvidence evidence : evidences) {
                candidates.addAll(evidence.faultCandidates());
            }

            String finalFault = candidates.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .collect(Collectors.groupingBy(item -> item, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(ruleResult.faultType());

            double ruleWeight = evidences.isEmpty() ? RULE_WEIGHT_ALONE : RULE_WEIGHT_WITH_AGENTS;
            double agentWeight = evidences.isEmpty() ? AGENT_WEIGHT_FALLBACK : AGENT_WEIGHT_WITH_RULES;
            double agentConfidence = evidences.stream().mapToDouble(AgentEvidence::confidence).average().orElse(0.45);
            double confidence = ruleWeight * ruleResult.confidence() + agentWeight * agentConfidence;

            List<String> decisionPath = List.of(
                    evidences.isEmpty() ? "规则预处理层直接命中" : "规则预处理层 + 多智能体协同",
                    "迟融合权重：规则 " + (int) (ruleWeight * 100) + "% / Agent " + (int) (agentWeight * 100) + "%");

            String report = "综合结论：" + finalFault + "\n建议：" + ruleResult.suggestion();
            List<String> possibleCauses = candidates.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .distinct()
                    .limit(5)
                    .toList();
            return new FusionDecision(finalFault, confidence, report, possibleCauses, decisionPath);
        }
    }

    public static class SafetyNetGate {
        public SafetyNetDecision apply(FusionDecision decision) {
            if (decision.confidence() >= SAFETY_NET_THRESHOLD) {
                return new SafetyNetDecision("VALIDATED", decision.confidence(), decision.report());
            }
            String report = decision.report() + "\n\n⚠️ 置信度不足，进入人工复核流程。";
            return new SafetyNetDecision("SUSPENDED", decision.confidence(), report);
        }
    }

    public static class PrivacyFilter {
        private static final Pattern VIN_PATTERN = Pattern.compile("\\b[A-HJ-NPR-Z0-9]{17}\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern PLATE_PATTERN = Pattern.compile(
                "[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼]"
                        + "[A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳]?",
                Pattern.CASE_INSENSITIVE);

        public PrivacyResult mask(String description) {
            Map<String, String> tokens = new LinkedHashMap<>();
            String masked = maskInternal(description, VIN_PATTERN, "VIN", tokens);
            masked = maskInternal(masked, PLATE_PATTERN, "PLATE", tokens);
            return new PrivacyResult(masked, tokens);
        }

        private String maskInternal(String text, Pattern pattern, String prefix, Map<String, String> tokens) {
            if (text == null || text.isBlank()) {
                return text == null ? "" : text;
            }
            Matcher matcher = pattern.matcher(text);
            StringBuffer buffer = new StringBuffer();
            int index = 1;
            while (matcher.find()) {
                String placeholder = "[" + prefix + "_" + index + "]";
                tokens.put(placeholder, matcher.group());
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(placeholder));
                index++;
            }
            matcher.appendTail(buffer);
            return buffer.toString();
        }
    }

    public record PrivacyResult(String maskedDescription, Map<String, String> tokens) {
        public String restore(String text) {
            if (text == null || tokens == null || tokens.isEmpty()) {
                return text;
            }
            String restored = text;
            for (Map.Entry<String, String> entry : tokens.entrySet()) {
                restored = restored.replace(entry.getKey(), entry.getValue());
            }
            return restored;
        }
    }

    public static class FaultKeywordMatcher {
        private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.ofEntries(
                Map.entry("启动系统", List.of("无法启动", "打不着火", "启动困难")),
                Map.entry("制动系统", List.of("刹车", "制动")),
                Map.entry("发动机", List.of("发动机", "怠速", "抖动", "异响")),
                Map.entry("变速箱/传动", List.of("变速箱", "顿挫", "打滑", "离合")),
                Map.entry("空调系统", List.of("空调", "不制冷", "异味")),
                Map.entry("电气安全", List.of("冒烟", "烧焦", "短路"))
        );

        public static List<String> extractCandidates(String text) {
            if (text == null || text.isBlank()) {
                return List.of();
            }
            Set<String> result = new LinkedHashSet<>();
            CATEGORY_KEYWORDS.forEach((category, keywords) -> {
                for (String keyword : keywords) {
                    if (text.contains(keyword)) {
                        result.add(category + "异常");
                        break;
                    }
                }
            });
            return new ArrayList<>(result);
        }

        public static List<String> extractKeywords(String description, List<String> fallbackSignals) {
            Set<String> keywords = new LinkedHashSet<>();
            if (description != null) {
                CATEGORY_KEYWORDS.values().stream()
                        .flatMap(List::stream)
                        .filter(description::contains)
                        .forEach(keywords::add);
            }
            if (keywords.isEmpty() && fallbackSignals != null) {
                fallbackSignals.stream()
                        .filter(Objects::nonNull)
                        .forEach(keywords::add);
            }
            return new ArrayList<>(keywords);
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record RulePattern(String label,
                              Pattern pattern,
                              String faultType,
                              String suggestion,
                              double confidence) {
        public boolean matches(String description) {
            if (description == null || description.isBlank()) {
                return false;
            }
            return pattern.matcher(description).find();
        }
    }
}

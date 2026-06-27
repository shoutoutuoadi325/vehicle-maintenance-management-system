package org.com.repair.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.repair.entity.AgentPromptTemplateConfig;
import org.com.repair.entity.DispatchWeightConfig;
import org.com.repair.repository.AgentPromptTemplateConfigRepository;
import org.com.repair.repository.DispatchWeightConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feedback-driven no-retraining self-iteration service.
 *
 * Core idea: "heavy decision, light training". The service shows how deposited
 * feedback samples can be aggregated with SQL and converted into prompt-template and
 * dispatch-weight suggestions.
 *
 * Configuration update path:
 * 1. scheduled or admin-triggered draft generation;
 * 2. admin review/approval through the backoffice API;
 * 3. approved values are written to agent_prompt_template_config and dispatch_weight_config.
 */
@Service
public class FeedbackSelfIterationService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackSelfIterationService.class);
    private static final int DEFAULT_SAMPLE_WINDOW_DAYS = 30;
    private static final String DEFAULT_AGENT_ROLE = "ARBITRATOR";
    private static final String DEFAULT_TEMPLATE_KEY = "default";
    private static final String DEFAULT_DISPATCH_CONFIG_KEY = "default";

    private final JdbcTemplate jdbcTemplate;
    private final AgentPromptTemplateConfigRepository promptTemplateRepository;
    private final DispatchWeightConfigRepository dispatchWeightConfigRepository;
    private volatile SelfIterationDraft latestDraft;

    public FeedbackSelfIterationService(
            JdbcTemplate jdbcTemplate,
            AgentPromptTemplateConfigRepository promptTemplateRepository,
            DispatchWeightConfigRepository dispatchWeightConfigRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.promptTemplateRepository = promptTemplateRepository;
        this.dispatchWeightConfigRepository = dispatchWeightConfigRepository;
    }

    /**
     * Builds a self-iteration draft from recent feedback.
     *
     * The draft contains feedback aggregates, prompt-template reinforcement content,
     * dispatch-weight suggestions and SQL statements for reviewed configuration updates.
     */
    @Transactional(readOnly = true)
    public SelfIterationDraft buildIterationDraft() {
        FeedbackAggregate aggregate = loadFeedbackAggregate(DEFAULT_SAMPLE_WINDOW_DAYS);
        DispatchWeightDraft weightDraft = proposeDispatchWeights(aggregate);

        String promptPatch = """
                当近30天低分反馈占比升高时，专家 Agent 提示词应增加：
                - 对车主描述中的遗漏症状进行追问；
                - 对低置信度结论输出人工复核建议；
                - 对制动、高温、漏油、电气短路等高风险场景强制安全兜底。
                """;

        SelfIterationDraft draft = new SelfIterationDraft(
                LocalDateTime.now(),
                aggregate,
                promptPatch,
                weightDraft,
                buildConfigUpsertPreviewSql(promptPatch, weightDraft));
        latestDraft = draft;
        return draft;
    }

    @Transactional(readOnly = true)
    public Optional<SelfIterationDraft> getLatestDraft() {
        return Optional.ofNullable(latestDraft);
    }

    /**
     * Keeps a fresh review draft ready for admins. Approval still requires a manual
     * API action so scheduled analysis never mutates live scoring or prompt config.
     */
    @Scheduled(cron = "0 15 3 * * *")
    public void refreshScheduledIterationDraft() {
        try {
            SelfIterationDraft draft = buildIterationDraft();
            logger.info("Generated scheduled feedback self-iteration draft, totalFeedback={}, lowRatingRatio={}",
                    draft.aggregate().totalFeedback(),
                    draft.aggregate().lowRatingRatio());
        } catch (Exception e) {
            logger.warn("Failed to generate scheduled feedback self-iteration draft", e);
        }
    }

    /**
     * Applies the latest reviewed draft to the live configuration tables.
     */
    @Transactional
    public SelfIterationApprovalResult approveLatestDraft(String reviewer, String reviewNote) {
        SelfIterationDraft draft = Optional.ofNullable(latestDraft)
                .orElseThrow(() -> new IllegalStateException("暂无待审核自演进草案，请先生成草案"));

        AgentPromptTemplateConfig promptConfig = promptTemplateRepository
                .findByAgentRoleAndTemplateKey(DEFAULT_AGENT_ROLE, DEFAULT_TEMPLATE_KEY)
                .orElseGet(() -> AgentPromptTemplateConfig.builder()
                        .agentRole(DEFAULT_AGENT_ROLE)
                        .templateKey(DEFAULT_TEMPLATE_KEY)
                        .promptTemplate("ARBITRATOR 默认会诊提示词")
                        .enabled(true)
                        .build());
        promptConfig.setEnabled(true);
        promptConfig.setSampleWindowDays(draft.aggregate().sampleWindowDays());
        promptConfig.setPromptTemplate(appendPromptPatch(promptConfig.getPromptTemplate(), draft.promptPatch()));
        promptConfig.setUpdateReason(buildReviewReason(reviewer, reviewNote));
        AgentPromptTemplateConfig savedPromptConfig = promptTemplateRepository.save(promptConfig);

        DispatchWeightConfig dispatchConfig = dispatchWeightConfigRepository
                .findByConfigKey(DEFAULT_DISPATCH_CONFIG_KEY)
                .orElseGet(() -> DispatchWeightConfig.builder()
                        .configKey(DEFAULT_DISPATCH_CONFIG_KEY)
                        .enabled(true)
                        .build());
        dispatchConfig.setEnabled(true);
        dispatchConfig.setSampleWindowDays(draft.aggregate().sampleWindowDays());
        dispatchConfig.setRatingWeight(draft.dispatchWeightDraft().ratingWeight());
        dispatchConfig.setWorkloadWeight(draft.dispatchWeightDraft().workloadWeight());
        dispatchConfig.setExperienceWeight(draft.dispatchWeightDraft().experienceWeight());
        dispatchConfig.setFatiguePenaltyWeight(draft.dispatchWeightDraft().fatiguePenaltyWeight());
        dispatchConfig.setUpdateReason(buildReviewReason(reviewer, reviewNote));
        DispatchWeightConfig savedDispatchConfig = dispatchWeightConfigRepository.save(dispatchConfig);

        SelfIterationApprovalResult result = new SelfIterationApprovalResult(
                LocalDateTime.now(),
                true,
                normalizeReviewer(reviewer),
                normalizeReviewNote(reviewNote),
                savedPromptConfig.getId(),
                savedDispatchConfig.getId(),
                draft.aggregate(),
                draft.promptPatch(),
                draft.dispatchWeightDraft(),
                "反馈自演进草案已审批并写入配置表");
        latestDraft = null;
        return result;
    }

    /**
     * SQL aggregate over deposited feedback samples.
     *
     * The query tracks rating
     * distribution, low-score ratio, and technician coverage are enough to justify
     * prompt reinforcement and dispatch-weight adjustment without model retraining.
     */
    private FeedbackAggregate loadFeedbackAggregate(int sampleWindowDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(sampleWindowDays);
        String sql = """
                SELECT
                    COUNT(*) AS total_feedback,
                    COALESCE(AVG(f.rating), 0) AS avg_rating,
                    SUM(CASE WHEN f.rating <= 2 THEN 1 ELSE 0 END) AS low_rating_count,
                    COUNT(DISTINCT t.id) AS touched_technicians
                FROM feedback f
                JOIN repair_order r ON r.id = f.repair_order_id
                LEFT JOIN order_technician ot ON ot.order_id = r.id
                LEFT JOIN technician t ON t.id = ot.technician_id
                WHERE f.rating IS NOT NULL
                  AND f.created_at >= ?
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long total = rs.getLong("total_feedback");
            long lowRating = rs.getLong("low_rating_count");
            double lowRatingRatio = total == 0 ? 0.0 : (double) lowRating / total;
            return new FeedbackAggregate(
                    sampleWindowDays,
                    total,
                    rs.getDouble("avg_rating"),
                    lowRating,
                    round(lowRatingRatio),
                    rs.getLong("touched_technicians"));
        }, cutoffTime);
    }

    /**
     * Produces a dispatch-weight draft.
     *
     * Example decision rule: when low-score feedback exceeds 20%, increase rating
     * and fatigue-safety influence, then slightly reduce workload/experience weights.
     * The values are normalized so the positive scoring weights still sum to 1.0.
     */
    private DispatchWeightDraft proposeDispatchWeights(FeedbackAggregate aggregate) {
        double ratingWeight = aggregate.lowRatingRatio() >= 0.20 ? 0.55 : 0.50;
        double workloadWeight = aggregate.lowRatingRatio() >= 0.20 ? 0.25 : 0.30;
        double experienceWeight = 1.0 - ratingWeight - workloadWeight;
        double fatiguePenaltyWeight = aggregate.lowRatingRatio() >= 0.20 ? 0.10 : 0.00;

        return new DispatchWeightDraft(
                decimal(ratingWeight),
                decimal(workloadWeight),
                decimal(experienceWeight),
                decimal(fatiguePenaltyWeight),
                "根据低分反馈占比生成派单权重调整建议。");
    }

    /**
     * Generates SQL text for updating config tables after manual approval.
     *
     * The SQL is used by management review flows to persist prompt-template and
     * dispatch-weight decisions.
     */
    private List<String> buildConfigUpsertPreviewSql(String promptPatch, DispatchWeightDraft weightDraft) {
        return List.of(
                """
                -- Update Agent prompt template after manual approval.
                UPDATE agent_prompt_template_config
                SET prompt_template = CONCAT(prompt_template, '\\n', '<DRY_RUN_PATCH>'),
                    update_reason = 'feedback aggregate proposal',
                    update_time = NOW()
                WHERE agent_role = 'ARBITRATOR'
                  AND template_key = 'default'
                  AND enabled = 1;
                """.replace("<DRY_RUN_PATCH>", promptPatch.replace("'", "''")),
                """
                -- Update dispatch weights after manual approval.
                UPDATE dispatch_weight_config
                SET rating_weight = %s,
                    workload_weight = %s,
                    experience_weight = %s,
                    fatigue_penalty_weight = %s,
                    update_reason = 'feedback aggregate proposal',
                    update_time = NOW()
                WHERE config_key = 'default'
                  AND enabled = 1;
                """.formatted(
                        weightDraft.ratingWeight(),
                        weightDraft.workloadWeight(),
                        weightDraft.experienceWeight(),
                        weightDraft.fatiguePenaltyWeight()));
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private String appendPromptPatch(String currentPrompt, String promptPatch) {
        String basePrompt = currentPrompt == null || currentPrompt.isBlank()
                ? "ARBITRATOR 默认会诊提示词"
                : currentPrompt.strip();
        String patchBlock = "[反馈自演进补丁 " + LocalDateTime.now().withNano(0) + "]\n" + promptPatch.strip();
        return basePrompt + "\n\n" + patchBlock;
    }

    private String buildReviewReason(String reviewer, String reviewNote) {
        String note = normalizeReviewNote(reviewNote);
        String reason = "feedback aggregate proposal approved by " + normalizeReviewer(reviewer);
        if (!note.isBlank()) {
            reason += ": " + note;
        }
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }

    private String normalizeReviewer(String reviewer) {
        return reviewer == null || reviewer.isBlank() ? "admin" : reviewer.strip();
    }

    private String normalizeReviewNote(String reviewNote) {
        return reviewNote == null ? "" : reviewNote.strip();
    }

    public record FeedbackAggregate(
            int sampleWindowDays,
            long totalFeedback,
            double averageRating,
            long lowRatingCount,
            double lowRatingRatio,
            long touchedTechnicians
    ) {
    }

    public record DispatchWeightDraft(
            BigDecimal ratingWeight,
            BigDecimal workloadWeight,
            BigDecimal experienceWeight,
            BigDecimal fatiguePenaltyWeight,
            String reason
    ) {
    }

    public record SelfIterationDraft(
            LocalDateTime generatedAt,
            FeedbackAggregate aggregate,
            String promptPatch,
            DispatchWeightDraft dispatchWeightDraft,
            List<String> configUpsertPreviewSql
    ) {
    }

    public record SelfIterationApprovalResult(
            LocalDateTime approvedAt,
            boolean approved,
            String reviewer,
            String reviewNote,
            Long promptTemplateConfigId,
            Long dispatchWeightConfigId,
            FeedbackAggregate aggregate,
            String appliedPromptPatch,
            DispatchWeightDraft appliedDispatchWeightDraft,
            String message
    ) {
    }
}

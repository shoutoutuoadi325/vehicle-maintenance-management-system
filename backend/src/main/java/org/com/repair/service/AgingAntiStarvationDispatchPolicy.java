package org.com.repair.service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Technician;

/**
 * 基于 Aging 的反饥饿调度策略。
 */
public class AgingAntiStarvationDispatchPolicy {

    /** 每等待 1 小时，priority 增长 0.08。 */
    private static final double WAIT_PENALTY_PER_HOUR = 0.08;

    /** 等待惩罚乘数上限，避免排序权重无限放大。 */
    private static final double MAX_WAIT_PENALTY_MULTIPLIER = 3.0;

    /** 长尾工单加成。 */
    private static final double LONG_TAIL_BONUS = 0.35;

    /** 生成 priority 老化更新 SQL 模板。 */
    public String buildPriorityAgingSqlTemplate() {
        return """
                -- 根据工单等待时长线性累加 priority
                UPDATE repair_order
                SET priority = LEAST(
                    3.0,
                    1.0 + TIMESTAMPDIFF(HOUR, created_at, NOW()) * 0.08
                )
                WHERE status = 'PENDING';
                """;
    }

    /** 按 Aging 优先级对待分配工单排序。 */
    public List<AgingOrderPriority> rankPendingOrders(List<RepairOrder> pendingOrders, Date now) {
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            return List.of();
        }

        Date safeNow = now != null ? now : new Date();
        return pendingOrders.stream()
                .map(order -> toPriority(order, safeNow))
                .sorted(Comparator.comparingDouble(AgingOrderPriority::priority).reversed())
                .collect(Collectors.toList());
    }

    /** 综合调度分 = 技师基础分 * 等待惩罚乘数 + 长尾加成 + 技能匹配加成。 */
    public double calculateDispatchScore(double baseTechnicianScore,
                                         RepairOrder order,
                                         Technician technician,
                                         Date now) {
        double waitPenaltyMultiplier = calculateWaitPenaltyMultiplier(order, now);
        double longTailBonus = isLongTailOrder(order) ? LONG_TAIL_BONUS : 0.0;
        double skillMatchBonus = isSkillMatched(order, technician) ? 0.2 : 0.0;

        return round(baseTechnicianScore * waitPenaltyMultiplier + longTailBonus + skillMatchBonus);
    }

    /** 等待惩罚乘数随工单驻留时间线性增长，并受上限约束。 */
    public double calculateWaitPenaltyMultiplier(RepairOrder order, Date now) {
        if (order == null || order.getCreatedAt() == null) {
            return 1.0;
        }

        Date safeNow = now != null ? now : new Date();
        long waitMillis = Math.max(0L, safeNow.getTime() - order.getCreatedAt().getTime());
        double waitHours = TimeUnit.MILLISECONDS.toMinutes(waitMillis) / 60.0;
        double multiplier = 1.0 + waitHours * WAIT_PENALTY_PER_HOUR;

        return round(Math.min(MAX_WAIT_PENALTY_MULTIPLIER, multiplier));
    }

    /** 根据工时、技能类型和故障关键词识别长尾工单。 */
    public boolean isLongTailOrder(RepairOrder order) {
        if (order == null) {
            return false;
        }

        boolean estimatedLong = order.getEstimatedHours() != null && order.getEstimatedHours() >= 6.0;
        boolean uncommonSkill = order.getRequiredSkillType() == Technician.SkillType.BODY_WORK
                || order.getRequiredSkillType() == Technician.SkillType.PAINT;

        String description = order.getDescription() == null
                ? ""
                : order.getDescription().toLowerCase(Locale.ROOT);
        boolean keywordMatched = description.contains("疑难")
                || description.contains("间歇")
                || description.contains("异响")
                || description.contains("偶发")
                || description.contains("复杂")
                || description.contains("rare")
                || description.contains("intermittent");

        return estimatedLong || uncommonSkill || keywordMatched;
    }

    private AgingOrderPriority toPriority(RepairOrder order, Date now) {
        double waitPenaltyMultiplier = calculateWaitPenaltyMultiplier(order, now);
        boolean longTailOrder = isLongTailOrder(order);
        double priority = waitPenaltyMultiplier + (longTailOrder ? LONG_TAIL_BONUS : 0.0);

        return new AgingOrderPriority(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getRequiredSkillType() != null ? order.getRequiredSkillType().name() : null,
                waitPenaltyMultiplier,
                longTailOrder,
                round(priority));
    }

    private boolean isSkillMatched(RepairOrder order, Technician technician) {
        if (order == null || technician == null) {
            return false;
        }
        return order.getRequiredSkillType() != null
                && order.getRequiredSkillType() == technician.getSkillType();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Aging 队列优先级结果。 */
    public record AgingOrderPriority(
            Long orderId,
            String orderNumber,
            String status,
            String requiredSkillType,
            double waitPenaltyMultiplier,
            boolean longTailOrder,
            double priority
    ) {
    }
}

package org.com.repair.service.green;

/**
 * 绿色碳排放计算所需的工单数据快照。
 *
 * 只保留计算相关字段，避免计算模型直接依赖完整工单对象。
 */
public record GreenOrderCarbonSnapshot(
        Double estimatedHours,
        boolean ecoMaterial,
        int reworkCount,
        String repairType,
        Double historicalEmission
) {

    /**
     * 获取安全工时，缺省时按 1 小时作为基准值。
     */
    public double safeEstimatedHours() {
        return estimatedHours != null ? estimatedHours : 1.0;
    }

    /**
     * 获取安全返工次数，避免负数进入排放计算。
     */
    public int safeReworkCount() {
        return Math.max(0, reworkCount);
    }

    /**
     * 判断当前维保策略是否为修复。
     */
    public boolean isRepairStrategy() {
        return "repair".equalsIgnoreCase(repairType);
    }
}

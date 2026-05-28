package org.com.repair.service.green;

/**
 * 碳排放系数配置快照。
 *
 * 集中描述材料、维保策略、返工惩罚等核心系数，并通过统一 key
 * 与数据库规则配置保持一致，便于后续调参和校准。
 */
public record CarbonFactorConfigSnapshot(
        double ecoMaterialFactor,
        double normalMaterialFactor,
        double repairStrategyFactor,
        double replaceStrategyFactor,
        double reworkPenaltyFactor
) {

    // 配置表中的规则 key，分别对应公式中的各类碳排放因子。
    public static final String KEY_ECO_MATERIAL_FACTOR = "CARBON_ECO_MATERIAL_FACTOR";
    public static final String KEY_NORMAL_MATERIAL_FACTOR = "CARBON_NORMAL_MATERIAL_FACTOR";
    public static final String KEY_REPAIR_STRATEGY_FACTOR = "CARBON_REPAIR_STRATEGY_FACTOR";
    public static final String KEY_REPLACE_STRATEGY_FACTOR = "CARBON_REPLACE_STRATEGY_FACTOR";
    public static final String KEY_REWORK_PENALTY_FACTOR = "CARBON_REWORK_PENALTY_FACTOR";

    /**
     * 返回当前模型的基准系数。
     */
    public static CarbonFactorConfigSnapshot defaultBaseline() {
        return new CarbonFactorConfigSnapshot(
                1.0,
                2.0,
                1.0,
                2.0,
                1.5
        );
    }
}

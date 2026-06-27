package org.com.repair.service.green;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * 绿色维修碳排放计算引擎。
 *
 * 模块职责：
 * 1. 按配置化系数计算工单相对碳排放。
 * 2. 将排放值映射为 S/A/B/C 绿色指数。
 * 3. 维护工单生命周期中的碳数据重算节点。
 * 4. 基于历史样本进行线性回归系数校准。
 */
@Service
public class GreenEmissionEngine {

    /**
     * 根据工单快照和系数配置计算相对碳排放。
     *
     * 公式：
     * 预估工时 * 材料因子 * 维保策略因子 + 返工次数 * 返工惩罚因子。
     */
    public double calculateRelativeEmission(
            GreenOrderCarbonSnapshot order,
            CarbonFactorConfigSnapshot config
    ) {
        CarbonFactorConfigSnapshot activeConfig = config != null
                ? config
                : CarbonFactorConfigSnapshot.defaultBaseline();

        // 材料因子：环保材料低碳，常规材料排放权重更高。
        double materialFactor = order.ecoMaterial()
                ? activeConfig.ecoMaterialFactor()
                : activeConfig.normalMaterialFactor();

        // 策略因子：修复优先，更换策略的碳权重更高。
        double strategyFactor = order.isRepairStrategy()
                ? activeConfig.repairStrategyFactor()
                : activeConfig.replaceStrategyFactor();

        return order.safeEstimatedHours() * materialFactor * strategyFactor
                + order.safeReworkCount() * activeConfig.reworkPenaltyFactor();
    }

    /**
     * 将排放值映射为绿色指数等级。
     *
     * 等级越靠前，代表低碳表现越好；建议文本用于前端或后台给出决策提示。
     */
    public GreenIndexMappingResult mapToGreenIndex(double estimatedEmission) {
        if (estimatedEmission <= 2.0) {
            return new GreenIndexMappingResult(
                    GreenIndexGrade.S,
                    estimatedEmission,
                    "Prefer repair-first strategy and keep using eco materials."
            );
        }
        if (estimatedEmission <= 5.0) {
            return new GreenIndexMappingResult(
                    GreenIndexGrade.A,
                    estimatedEmission,
                    "Low-carbon result is good; review whether reused parts are available."
            );
        }
        if (estimatedEmission <= 8.0) {
            return new GreenIndexMappingResult(
                    GreenIndexGrade.B,
                    estimatedEmission,
                    "Standard result; reduce rework and optimize material selection."
            );
        }
        return new GreenIndexMappingResult(
                GreenIndexGrade.C,
                estimatedEmission,
                "High-emission order; suggest repair plan review before replacement."
        );
    }

    /**
     * 生成工单生命周期中的碳数据重算节点。
     *
     * 这些节点用于描述从创建、字段更新、完工校验到事件结算的完整一致性链路。
     */
    public List<String> buildLifecycleRecalculationPlan() {
        List<String> plan = new ArrayList<>();
        plan.add("CREATE_ORDER: calculate estimated_emission and initial green_index.");
        plan.add("UPDATE_ORDER_GREEN_FIELDS: recalculate when ecoMaterial/reworkCount/repairType changes.");
        plan.add("STATUS_TO_COMPLETED: verify final actualHours/materialCost before settlement.");
        plan.add("AFTER_COMMIT: publish green reward event after carbon data is persisted.");
        return plan;
    }

    /**
     * 使用多元线性回归校准碳排放影响系数。
     *
     * features 可包含预估工时、材料标记、策略标记、返工次数等特征；
     * targets 为历史实测排放值；返回的 weights 表示校准后的影响权重。
     */
    public double[] calibrateFactorsByLinearRegression(
            double[][] features,
            double[] targets,
            int iterations,
            double learningRate
    ) {
        if (features == null || targets == null || features.length == 0 || features.length != targets.length) {
            return new double[0];
        }

        int columnCount = features[0].length;
        double[] weights = new double[columnCount];
        int safeIterations = Math.max(1, iterations);
        double safeLearningRate = learningRate > 0 ? learningRate : 0.001;

        // 使用批量梯度下降逐步缩小预测值与实测值之间的误差。
        for (int iteration = 0; iteration < safeIterations; iteration++) {
            double[] gradients = new double[columnCount];
            for (int row = 0; row < features.length; row++) {
                double predicted = dot(features[row], weights);
                double error = predicted - targets[row];
                for (int column = 0; column < columnCount; column++) {
                    gradients[column] += error * features[row][column];
                }
            }

            // 按学习率更新每个特征对应的权重。
            for (int column = 0; column < columnCount; column++) {
                weights[column] -= safeLearningRate * gradients[column] / features.length;
            }
        }

        return weights;
    }

    /**
     * 计算单条样本特征与权重的点积，作为线性模型预测值。
     */
    private double dot(double[] row, double[] weights) {
        double value = 0.0;
        int limit = Math.min(row.length, weights.length);
        for (int index = 0; index < limit; index++) {
            value += row[index] * weights[index];
        }
        return value;
    }
}

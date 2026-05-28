package org.com.repair.service.green;

/**
 * 绿色指数映射结果。
 *
 * 包含等级、原始排放值和低碳建议，方便应用层直接读取。
 */
public record GreenIndexMappingResult(
        GreenIndexGrade grade,
        double estimatedEmission,
        String recommendation
) {
}

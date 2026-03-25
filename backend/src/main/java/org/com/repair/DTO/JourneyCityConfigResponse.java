package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "零碳旅程城市节点配置")
public record JourneyCityConfigResponse(
        @Schema(description = "节点索引", example = "0")
        Integer cityIndex,

        @Schema(description = "城市名称", example = "成都")
        String cityName,

        @Schema(description = "节点里程", example = "120")
        Integer requiredMileage,

        @Schema(description = "SVG横坐标", example = "285")
        Integer x,

        @Schema(description = "SVG纵坐标", example = "345")
        Integer y
) {
}
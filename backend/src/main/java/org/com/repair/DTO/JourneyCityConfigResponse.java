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
        Integer y,

        @Schema(description = "是否为合作品牌服务区", example = "true")
        Boolean brandServiceArea,

        @Schema(description = "品牌名称", example = "壳牌")
        String brandName,

        @Schema(description = "品牌Logo", example = "/assets/brands/shell.svg")
        String brandLogoUrl
) {
}
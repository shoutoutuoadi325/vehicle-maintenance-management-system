package org.com.repair.DTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "零碳公路路线配置")
public record JourneyConfigResponse(
        @Schema(description = "可选路线列表")
        List<JourneyMapSummaryResponse> maps,

        @Schema(description = "用户当前路线ID", example = "1")
        Long currentMapId,

        @Schema(description = "城市节点配置")
        List<JourneyCityConfigResponse> nodes
) {
}
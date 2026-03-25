package org.com.repair.DTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "零碳公路路线配置")
public record JourneyConfigResponse(
        @Schema(description = "城市节点配置")
        List<JourneyCityConfigResponse> nodes
) {
}
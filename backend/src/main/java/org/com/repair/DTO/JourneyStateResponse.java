package org.com.repair.DTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "零碳公路全量状态")
public record JourneyStateResponse(
        @Schema(description = "用户ID", example = "1001")
        Long userId,

        @Schema(description = "当前总能量", example = "220")
        Integer totalEnergy,

        @Schema(description = "当前里程", example = "220")
        Integer currentMileage,

        @Schema(description = "节点状态列表")
        List<JourneyNodeResponse> nodes
) {
}

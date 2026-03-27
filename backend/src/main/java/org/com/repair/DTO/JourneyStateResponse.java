package org.com.repair.DTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "零碳公路全量状态")
public record JourneyStateResponse(
        @Schema(description = "用户ID", example = "1001")
        Long userId,

        @Schema(description = "当前路线ID", example = "1")
        Long currentMapId,

        @Schema(description = "当前路线名称", example = "318川藏线")
        String currentMapName,

        @Schema(description = "旅程状态：NORMAL/PENDING_RANDOM_EVENT", example = "NORMAL")
        String journeyStatus,

        @Schema(description = "冻结中的里程累计值", example = "20")
        Integer frozenMileage,

        @Schema(description = "当前总能量", example = "220")
        Integer totalEnergy,

        @Schema(description = "当前里程", example = "220")
        Integer currentMileage,

        @Schema(description = "当前路线总进度百分比", example = "37.58")
        Double progressPercent,

        @Schema(description = "距离下一站剩余百分比", example = "62.42")
        Double remainingToNextStationPercent,

        @Schema(description = "车辆当前虚拟坐标X", example = "312.4")
        Double currentX,

        @Schema(description = "车辆当前虚拟坐标Y", example = "336.2")
        Double currentY,

        @Schema(description = "节点状态列表")
        List<JourneyNodeResponse> nodes
) {
}

package org.com.repair.DTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "零碳先锋排行榜")
public record LeaderboardResponse(
        @Schema(description = "排行类型", example = "TOTAL_ENERGY")
        String type,

        @Schema(description = "榜单生成时间", example = "2026-03-28T10:00:00")
        String updatedAt,

        @Schema(description = "榜单数据")
        List<LeaderboardItemResponse> items
) {
}

package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "零碳先锋排行榜单项")
public record LeaderboardItemResponse(
        @Schema(description = "排名", example = "1")
        int rank,

        @Schema(description = "用户ID", example = "1001")
        Long userId,

        @Schema(description = "脱敏用户名", example = "a***z")
        String maskedUsername,

        @Schema(description = "脱敏手机号", example = "138****1234")
        String maskedPhone,

        @Schema(description = "当前进行中的路线名称", example = "318川藏线")
        String currentMapName,

        @Schema(description = "累计总能量", example = "1260")
        Integer totalEnergy,

        @Schema(description = "当前行进总里程", example = "830")
        Integer currentMileage
) {
}

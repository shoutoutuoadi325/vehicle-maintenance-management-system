package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "选择零碳旅程路线响应")
public record JourneyMapSelectResponse(
        @Schema(description = "用户ID", example = "1001")
        Long userId,

        @Schema(description = "当前路线ID", example = "1")
        Long currentMapId,

        @Schema(description = "当前路线名称", example = "318川藏线")
        String currentMapName
) {
}

package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "路线概要信息")
public record JourneyMapSummaryResponse(
        @Schema(description = "路线ID", example = "1")
        Long mapId,

        @Schema(description = "路线名称", example = "318川藏线")
        String mapName,

        @Schema(description = "是否为用户当前路线", example = "true")
        Boolean selected
) {
}

package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "选择零碳旅程路线请求")
public record JourneyMapSelectRequest(
        @NotNull
        @Schema(description = "路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        Long mapId
) {
}

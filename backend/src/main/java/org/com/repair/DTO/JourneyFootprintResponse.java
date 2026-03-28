package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "旅行足迹项")
public record JourneyFootprintResponse(
        @Schema(description = "足迹ID", example = "1")
        Long id,

        @Schema(description = "路线ID", example = "1")
        Long mapId,

        @Schema(description = "事件类型", example = "CHECKIN_CITY")
        String eventType,

        @Schema(description = "事件描述", example = "成功抵达林芝")
        String eventDescription,

        @Schema(description = "创建时间", example = "2026-03-27T13:30:00")
        String createdAt
) {
}

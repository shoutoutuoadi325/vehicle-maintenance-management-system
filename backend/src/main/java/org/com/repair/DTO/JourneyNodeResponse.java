package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "旅程节点状态")
public record JourneyNodeResponse(
        @Schema(description = "节点索引", example = "0")
        Integer cityIndex,

        @Schema(description = "城市名称", example = "成都")
        String cityName,

        @Schema(description = "解锁所需里程", example = "0")
        Integer requiredMileage,

        @Schema(description = "节点状态：LOCKED/UNLOCKED/CHECKED_IN", example = "UNLOCKED")
        String nodeState
) {
}

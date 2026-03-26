package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通关终极奖励申领结果")
public record ClaimGrandPrizeResponse(
        @Schema(description = "用户ID", example = "1001")
        Long userId,

        @Schema(description = "是否已完成全程", example = "true")
        boolean journeyCompleted,

        @Schema(description = "是否已申领实体车贴", example = "true")
        boolean stickerClaimed,

        @Schema(description = "是否发放终极商业大奖", example = "true")
        boolean grandPrizeGranted,

        @Schema(description = "发货状态", example = "PREPARING")
        String shippingStatus
) {
}

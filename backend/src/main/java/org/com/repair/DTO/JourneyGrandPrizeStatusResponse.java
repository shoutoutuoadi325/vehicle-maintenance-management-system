package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "我的通关奖励状态")
public record JourneyGrandPrizeStatusResponse(
        @Schema(description = "用户ID", example = "1001")
        Long userId,

        @Schema(description = "是否完成全程", example = "true")
        boolean journeyCompleted,

        @Schema(description = "是否已申领实体车贴", example = "true")
        boolean stickerClaimed,

        @Schema(description = "是否发放终极商业大奖", example = "true")
        boolean grandPrizeGranted,

        @Schema(description = "发货状态", example = "PREPARING")
        String shippingStatus,

        @Schema(description = "收货人姓名", example = "张三")
        String consigneeName,

        @Schema(description = "联系电话", example = "13800138000")
        String consigneePhone,

        @Schema(description = "详细收货地址", example = "四川省成都市高新区天府大道88号")
        String shippingAddress,

        @Schema(description = "物流单号", example = "SF1234567890")
        String shipmentTrackingNo,

        @Schema(description = "通关时间", example = "2026-03-26T10:20:30")
        String completedAt,

        @Schema(description = "发货时间", example = "2026-03-28T10:20:30")
        String shippedAt
) {
}

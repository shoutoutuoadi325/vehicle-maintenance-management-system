package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "优惠券线下核销结果")
public record CouponRedeemResponse(
        @Schema(description = "卡包记录ID", example = "101")
        Long walletId,

        @Schema(description = "用户ID", example = "1001")
        Long userId,

        @Schema(description = "卡券状态", example = "REDEEMED")
        String couponStatus,

        @Schema(description = "核销时间", example = "2026-03-27T15:30:00")
        String redeemTime,

        @Schema(description = "核销门店ID", example = "3")
        Long shopId,

        @Schema(description = "核销技师ID", example = "18")
        Long technicianId
) {
}

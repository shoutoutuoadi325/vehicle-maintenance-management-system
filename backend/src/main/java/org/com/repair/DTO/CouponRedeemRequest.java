package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "优惠券线下核销请求")
public record CouponRedeemRequest(
        @NotNull
        @Schema(description = "卡包记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
        Long walletId,

        @Schema(description = "核销门店ID（可选，与 technicianId 至少传一个）", example = "3")
        Long shopId,

        @Schema(description = "核销技师ID（可选，与 shopId 至少传一个）", example = "18")
        Long technicianId
) {
}

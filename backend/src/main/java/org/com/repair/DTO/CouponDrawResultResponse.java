package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "城市服务区卡券抽奖结果")
public record CouponDrawResultResponse(
        @Schema(description = "是否中奖", example = "true")
        boolean won,

        @Schema(description = "优惠券ID", example = "100")
        Long couponId,

        @Schema(description = "合作品牌名称", example = "壳牌")
        String brandName,

        @Schema(description = "品牌Logo地址", example = "/assets/brands/shell.svg")
        String brandLogoUrl,

        @Schema(description = "优惠券标题", example = "壳牌机油免单一次")
        String couponTitle,

        @Schema(description = "优惠券描述", example = "成都服务区可兑换壳牌全合成机油保养一次")
        String couponDescription,

        @Schema(description = "城市节点索引", example = "0")
        Integer cityIndex
) {
}

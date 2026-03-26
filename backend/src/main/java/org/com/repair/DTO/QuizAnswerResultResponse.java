package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "答题结算结果")
public record QuizAnswerResultResponse(
    @Schema(description = "用户ID", example = "1001")
    Long userId,

    @Schema(description = "题目ID", example = "12")
    Long quizId,

    @Schema(description = "是否答对", example = "true")
    boolean correct,

    @Schema(description = "本次奖励能量", example = "10")
    int rewardEnergy,

    @Schema(description = "当前能量余额", example = "130")
    int totalEnergy,

    @Schema(description = "当前公路里程进度", example = "55")
    int currentMileage,

    @Schema(description = "抽奖结果")
    CouponDrawResultResponse couponDrawResult,

    @Schema(description = "是否已完成全程", example = "true")
    boolean journeyCompleted,

    @Schema(description = "是否已申领实体车贴", example = "false")
    boolean grandPrizeStickerClaimed,

    @Schema(description = "终极奖励发货状态", example = "NOT_CLAIMED")
    String grandPrizeShipmentStatus
) {
}

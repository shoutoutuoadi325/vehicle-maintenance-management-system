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
    int currentMileage
) {
}

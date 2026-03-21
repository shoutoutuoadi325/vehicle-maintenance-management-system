package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "城市节点打卡请求")
public record JourneyCheckinRequest(
        @NotNull
        @Schema(description = "用户ID", example = "1001")
        Long userId,

        @NotNull
        @Schema(description = "城市节点索引", example = "1")
        Integer cityIndex,

        @NotNull
        @Schema(description = "题目ID", example = "5")
        Long quizId,

        @NotBlank
        @Schema(description = "用户选择的答案", example = "B")
        String selectedAnswer
) {
}

package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "答题结算请求")
public record QuizAnswerRequest(
    @NotNull
    @Schema(description = "用户ID", example = "1001")
    Long userId,

    @NotNull
    @Schema(description = "题目ID", example = "12")
    Long quizId,

    @NotNull
    @Schema(description = "是否答对", example = "true")
    Boolean isCorrect
) {
}

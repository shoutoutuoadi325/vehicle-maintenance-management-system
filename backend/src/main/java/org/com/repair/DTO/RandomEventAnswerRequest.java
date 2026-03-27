package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "路途随机突发事件答题请求")
public record RandomEventAnswerRequest(
        @NotNull
        @Schema(description = "题目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
        Long quizId,

        @NotBlank
        @Schema(description = "用户选择的答案", requiredMode = Schema.RequiredMode.REQUIRED, example = "B")
        String selectedAnswer
) {
}

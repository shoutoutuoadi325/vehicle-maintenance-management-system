package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "环保问答题目（不包含正确答案）")
public record QuizQuestionResponse(
        @Schema(description = "题目ID", example = "1")
        Long id,

        @Schema(description = "城市节点索引", example = "2")
        Integer cityIndex,

        @Schema(description = "突发事件标题", example = "遭遇沙尘暴")
        String eventTitle,

        @Schema(description = "突发事件描述", example = "前方沙尘暴预警，关于空调滤芯的知识你了解吗？")
        String eventDescription,

        @Schema(description = "事件视觉主题", example = "sandstorm")
        String eventTheme,

        @Schema(description = "题干", example = "下列哪种驾驶习惯更有助于降低碳排放？")
        String question,

        @Schema(description = "选项JSON", example = "{\"A\":\"频繁急加速\",\"B\":\"平稳加速并保持匀速\"}")
        String options
) {
}

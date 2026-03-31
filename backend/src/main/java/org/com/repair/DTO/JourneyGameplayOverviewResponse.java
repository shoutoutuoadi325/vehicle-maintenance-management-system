package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "零碳公路之旅聚合概览")
public record JourneyGameplayOverviewResponse(
        @Schema(description = "路线与站点配置")
        JourneyConfigResponse config,

        @Schema(description = "当前旅程状态")
        JourneyStateResponse state,

        @Schema(description = "当前待处理的随机事件题（无则为null）")
        QuizQuestionResponse pendingRandomEvent,

        @Schema(description = "终极奖励状态")
        JourneyGrandPrizeStatusResponse grandPrizeStatus
) {
}

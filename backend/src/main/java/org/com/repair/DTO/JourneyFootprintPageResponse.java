package org.com.repair.DTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "旅行足迹分页响应")
public record JourneyFootprintPageResponse(
        @Schema(description = "当前页码（从0开始）", example = "0")
        int page,

        @Schema(description = "页大小", example = "10")
        int size,

        @Schema(description = "总记录数", example = "58")
        long totalElements,

        @Schema(description = "总页数", example = "6")
        int totalPages,

        @Schema(description = "足迹列表")
        List<JourneyFootprintResponse> items
) {
}

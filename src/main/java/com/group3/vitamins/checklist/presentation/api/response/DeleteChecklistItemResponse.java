package com.group3.vitamins.checklist.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeleteChecklistItemResponse(
        @Schema(description = "현재 완료된 항목 개수", example = "3")
        int completedCount,

        @Schema(description = "전체 항목 개수", example = "5")
        int totalCount
) {
}

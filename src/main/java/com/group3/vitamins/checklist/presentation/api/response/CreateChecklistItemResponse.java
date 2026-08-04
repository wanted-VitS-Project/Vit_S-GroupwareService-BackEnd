package com.group3.vitamins.checklist.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CreateChecklistItemResponse(
        @Schema(description = "항목이 생성된 체크리스트 블록 ID", example = "1")
        Long chkBlockId,

        @Schema(description = "생성된 체크리스트 항목 ID", example = "1")
        Long chkId,

        @Schema(description = "생성된 체크리스트 항목 내용", example = "제안서 결재 보고하기")
        String content,

        @Schema(description = "현재 완료된 항목 개수", example = "3")
        int completedCount,

        @Schema(description = "전체 항목 개수", example = "5")
        int totalCount,

        @Schema(description = "체크리스트 항목 생성일", example = "2026-08-03T09:12:44")
        LocalDateTime createdAt
) {
}

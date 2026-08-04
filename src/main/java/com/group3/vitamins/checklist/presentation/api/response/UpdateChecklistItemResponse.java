package com.group3.vitamins.checklist.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UpdateChecklistItemResponse(
        @Schema(description = "수정된 체크리스트 항목 ID", example = "1")
        Long chkId,

        @Schema(description = "수정된 체크리스트 항목 내용", example = "제안서 결재 보고하기(매우 중요!)")
        String content,

        @Schema(description = "수정된 체크리스트 완료 여부", example = "false")
        boolean isCompleted,

        @Schema(description = "현재 완료된 항목 개수", example = "3")
        int completedCount,

        @Schema(description = "전체 항목 개수", example = "5")
        int totalCount,

        @Schema(description = "체크리스트 항목 수정일")
        LocalDateTime updatedAt
) {
}

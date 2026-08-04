package com.group3.vitamins.checklist.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChecklistItemUpdateRequest(
        @Schema(description = "사용자가 수정한 부분을 포함한 모든 내용(nullable)", example = "제안서 결재 보고하기(매우 중요!)")
        String content,

        @Schema(description = "목표 완료 여부 상태(nullable)", example = "null")
        Boolean changeStatusTo
) {
}

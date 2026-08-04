package com.group3.vitamins.text.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record TextUpdateRequest(
        @Schema(description = "사용자가 수정한 부분을 포함한 모든 내용", example = "**오전 회의록** \n주제: 제안서 분담하기 \n기한: 오늘 오후",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String content
) {
}

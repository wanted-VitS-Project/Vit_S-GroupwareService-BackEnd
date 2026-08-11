package com.group3.vitamins.text.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UpdateTextContentResponse(
        @Schema(description = "수정된 텍스트 블록 ID", example = "1")
        Long txtId,

        @Schema(description = "수정된 텍스트 블록 내용", example = "**오전 회의록** \n주제: 제안서 분담하기 \n기한: 오늘 오후")
        String content,

        @Schema(description = "텍스트 블록 수정일")
        LocalDateTime updatedAt,

        @Schema(description = "수정 후 버전 — 다음 수정 요청에 그대로 실어 보낸다", example = "2")
        int version
) {
}

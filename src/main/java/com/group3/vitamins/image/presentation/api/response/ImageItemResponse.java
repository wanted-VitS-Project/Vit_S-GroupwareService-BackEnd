package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ImageItemResponse(
        @Schema(description = "생성된 이미지 ID", example = "10")
        Long imgId,

        @Schema(description = "원본 파일명", example = "image1.jpg")
        String originalName,

        @Schema(description = "저장소에 업로드된 이미지 URL", example = "https://s3.../abc.jpg")
        String imageUrl,

        @Schema(description = "이미지 캡션", example = "회의실 전경")
        String caption,

        @Schema(description = "이미지 순서", example = "1")
        int orderIndex,

        @Schema(description = "생성일", example = "2026-07-31T15:20:00")
        LocalDateTime createdAt
) {
}

package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ProjectImageItemResponse(
        @Schema(description = "속한 블록 ID", example = "3")
        Long imgBlockId,

        @Schema(description = "이미지 ID", example = "10")
        Long imgId,

        @Schema(description = "원본 파일명", example = "회의사진.jpg")
        String originalName,

        @Schema(description = "저장소 이미지 URL", example = "https://s3.../abc.jpg")
        String imageUrl,

        @Schema(description = "이미지 캡션", example = "회의실 전경")
        String caption,

        @Schema(description = "생성일", example = "2026-08-01T10:00:00")
        LocalDateTime createdAt
) {
}

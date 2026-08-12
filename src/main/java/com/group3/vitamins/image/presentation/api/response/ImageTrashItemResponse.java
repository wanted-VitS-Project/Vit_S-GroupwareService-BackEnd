package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ImageTrashItemResponse(
        @Schema(description = "이미지 ID", example = "10")
        Long imgId,

        @Schema(description = "원본 파일명", example = "회의사진.jpg")
        String originalName,

        @Schema(description = "저장소 이미지 URL", example = "https://s3.../abc.jpg")
        String imageUrl,

        @Schema(description = "이미지 캡션", example = "회의실 전경")
        String caption,

        @Schema(description = "삭제 일시", example = "2026-08-03T10:00:00")
        LocalDateTime deletedAt,

        @Schema(description = "상위 블록까지 삭제됐는지 — true면 복구 시도해도 IMG-009로 거부된다. "
                + "프론트가 복구 버튼을 미리 비활성화하는 용도(2026-08-11 추가)", example = "false")
        boolean blockDeleted
) {
}

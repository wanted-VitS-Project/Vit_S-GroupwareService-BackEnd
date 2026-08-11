package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RestoredImageItemResponse(
        @Schema(description = "복구된 이미지가 속한 블록 ID", example = "3")
        Long imgBlockId,

        @Schema(description = "복구된 이미지 ID", example = "10")
        Long imgId,

        @Schema(description = "원본 파일명", example = "회의사진.jpg")
        String originalName,

        @Schema(description = "복구 후 순서", example = "6")
        int orderIndex
) {
}

package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdatedImageOrderResponse(
        @Schema(description = "이미지 ID", example = "13")
        Long imgId,

        @Schema(description = "수정 후 순서", example = "1")
        int orderIndex,

        @Schema(description = "수정된 캡션", example = "회의실 전경")
        String caption
) {
}

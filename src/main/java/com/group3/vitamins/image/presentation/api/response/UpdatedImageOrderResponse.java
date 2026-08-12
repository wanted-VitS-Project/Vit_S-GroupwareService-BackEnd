package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdatedImageOrderResponse(
        @Schema(description = "이미지 ID", example = "13")
        Long imgId,

        @Schema(description = "수정 후 순서", example = "1")
        int orderIndex,

        @Schema(description = "수정된 캡션", example = "회의실 전경")
        String caption,

        @Schema(description = "수정 후 버전 — 다음 수정 요청에 그대로 실어 보낸다", example = "2")
        int version
) {
}

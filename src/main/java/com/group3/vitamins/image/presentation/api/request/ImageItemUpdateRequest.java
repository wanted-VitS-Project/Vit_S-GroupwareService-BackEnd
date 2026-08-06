package com.group3.vitamins.image.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImageItemUpdateRequest(
        @Schema(description = "정렬된 순서대로 나열된 이미지 목록(캡션 포함)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<Entry> images
) {
    public record Entry(
            @Schema(description = "이미지 ID", example = "13")
            Long imgId,

            @Schema(description = "이미지 캡션(없으면 빈 문자열로 저장됨)", example = "회의실 전경")
            String caption
    ) {
    }
}

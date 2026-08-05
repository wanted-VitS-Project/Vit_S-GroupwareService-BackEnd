package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CreateImageItemsResponse(
        @Schema(description = "항목이 생성된 이미지 블록 ID", example = "1")
        Long imgBlockId,

        @Schema(description = "업로드된 이미지 목록")
        List<ImageItemResponse> images
) {
}

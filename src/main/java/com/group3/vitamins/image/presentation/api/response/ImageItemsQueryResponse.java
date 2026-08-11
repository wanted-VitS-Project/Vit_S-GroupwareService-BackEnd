package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImageItemsQueryResponse(
        @Schema(description = "해당 블록의 전체(활성) 이미지 개수", example = "5")
        int totalCount,

        @Schema(description = "이미지 목록 (orderIndex 오름차순)")
        List<BlockImageItemResponse> images
) {
}

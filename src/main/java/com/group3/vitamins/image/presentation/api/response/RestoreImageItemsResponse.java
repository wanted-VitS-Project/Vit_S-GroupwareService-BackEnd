package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RestoreImageItemsResponse(
        @Schema(description = "복구된 이미지 목록")
        List<RestoredImageItemResponse> images
) {
}

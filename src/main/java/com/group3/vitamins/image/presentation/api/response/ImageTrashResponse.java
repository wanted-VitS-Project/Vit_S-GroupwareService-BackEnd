package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImageTrashResponse(
        @Schema(description = "삭제된 이미지 목록")
        List<ImageTrashItemResponse> images
) {
}

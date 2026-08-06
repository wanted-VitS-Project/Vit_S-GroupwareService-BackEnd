package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UpdateImageItemsResponse(
        @Schema(description = "순서가 반영된 이미지 목록")
        List<UpdatedImageOrderResponse> images
) {
}

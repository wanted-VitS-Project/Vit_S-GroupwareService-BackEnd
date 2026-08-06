package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProjectImagesResponse(
        @Schema(description = "프로젝트 내 전체 이미지 목록")
        List<ProjectImageItemResponse> images
) {
}

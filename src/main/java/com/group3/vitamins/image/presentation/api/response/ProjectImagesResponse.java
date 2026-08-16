package com.group3.vitamins.image.presentation.api.response;

import com.group3.vitamins.image.application.usecase.ImageQueryUseCase.ProjectImagesView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProjectImagesResponse(
        @Schema(description = "현재 페이지 번호 (0-base)", example = "0")
        int page,

        @Schema(description = "페이지당 개수", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "2")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages,

        @Schema(description = "프로젝트 내 이미지 목록 (현재 페이지분만)")
        List<ProjectImageItemResponse> images
) {

    public static ProjectImagesResponse from(ProjectImagesView view) {
        return new ProjectImagesResponse(
                view.page(), view.size(), view.totalElements(), view.totalPages(),
                view.images().stream()
                        .map(item -> new ProjectImageItemResponse(
                                item.imgBlockId(), item.blockTitle(), item.stepId(), item.stepName(),
                                item.imgId(), item.originalName(), item.imageUrl(), item.caption(), item.createdAt()))
                        .toList()
        );
    }
}

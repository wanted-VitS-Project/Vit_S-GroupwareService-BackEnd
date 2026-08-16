package com.group3.vitamins.image.presentation.api.response;

import com.group3.vitamins.image.application.usecase.ImageQueryUseCase.ImageTrashView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImageTrashResponse(
        @Schema(description = "현재 페이지 번호 (0-base)", example = "0")
        int page,

        @Schema(description = "페이지당 개수", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "1")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages,

        @Schema(description = "삭제된 이미지 목록 (현재 페이지분만)")
        List<ImageTrashItemResponse> images
) {

    public static ImageTrashResponse from(ImageTrashView view) {
        return new ImageTrashResponse(
                view.page(), view.size(), view.totalElements(), view.totalPages(),
                view.images().stream()
                        .map(item -> new ImageTrashItemResponse(
                                item.imgId(), item.originalName(), item.imageUrl(), item.caption(),
                                item.deletedAt(), item.blockDeleted()))
                        .toList()
        );
    }
}

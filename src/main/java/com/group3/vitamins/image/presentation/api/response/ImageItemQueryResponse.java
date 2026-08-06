package com.group3.vitamins.image.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageItemQueryResponse(
        @Schema(description = "조회된 이미지 ID", example = "10")
        Long imgId,

        @Schema(description = "원본 파일명", example = "image1.jpg")
        String originalName,

        @Schema(description = "저장소에 업로드된 이미지 URL", example = "https://s3.../abc.jpg")
        String imageUrl,

        @Schema(description = "이미지 캡션", example = "회의실 전경")
        String caption,

        @Schema(description = "이미지 정렬 번호", example = "2")
        int orderIndex,

        @Schema(description = "해당 블록의 전체 이미지 개수", example = "5")
        int totalCount
) {
}

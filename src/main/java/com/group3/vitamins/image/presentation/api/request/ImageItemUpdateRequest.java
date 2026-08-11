package com.group3.vitamins.image.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ImageItemUpdateRequest(
        @Valid
        @Schema(description = "정렬된 순서대로 나열된 이미지 목록(캡션 포함)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<Entry> images
) {
    public record Entry(
            @Schema(description = "이미지 ID", example = "13")
            Long imgId,

            @Schema(description = "이미지 캡션(없으면 빈 문자열로 저장됨)", example = "회의실 전경")
            String caption,

            @NotNull(message = "IMAGE_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
            @Schema(description = "이미지 항목 전체 조회에서 받은 version 을 그대로 실어 보낸다. "
                    + "이 배열 중 하나라도 그 사이 남이 먼저 저장했으면 요청 전체가 409 다", example = "1")
            Integer version
    ) {
    }
}

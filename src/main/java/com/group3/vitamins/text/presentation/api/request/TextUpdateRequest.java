package com.group3.vitamins.text.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TextUpdateRequest(
        @Schema(description = "사용자가 수정한 부분을 포함한 모든 내용", example = "**오전 회의록** \n주제: 제안서 분담하기 \n기한: 오늘 오후",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @NotNull(message = "TEXT_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
        @Schema(description = "블록 목록/상세 조회에서 받은 version 을 그대로 실어 보낸다. "
                + "그 사이 남이 먼저 저장했으면 409 다", example = "1")
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {
}

package com.group3.vitamins.image.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImageItemPurgeRequest(
        @Schema(description = "완전 삭제할 이미지 ID 목록(휴지통에 있는 것만)", requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> imgIds
) {
}

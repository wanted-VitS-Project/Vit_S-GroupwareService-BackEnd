package com.group3.vitamins.image.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImageItemRestoreRequest(
        @Schema(description = "복구할 이미지 ID 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> imgIds
) {
}

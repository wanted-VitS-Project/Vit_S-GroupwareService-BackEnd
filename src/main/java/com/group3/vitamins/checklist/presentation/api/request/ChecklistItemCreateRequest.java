package com.group3.vitamins.checklist.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ChecklistItemCreateRequest(
        @Schema(description = "체크리스트 항목에 담길 내용", example = "제안서 결재 보고하기")
        @NotBlank
        String content
) {
}

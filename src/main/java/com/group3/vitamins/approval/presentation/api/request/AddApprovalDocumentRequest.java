package com.group3.vitamins.approval.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddApprovalDocumentRequest(
        @Schema(description = "업로드 완료된 파일 버전 구분 번호", example = "10")
        @NotNull @Positive
        Long fileVersionId
) {
}

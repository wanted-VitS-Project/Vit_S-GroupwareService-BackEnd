package com.group3.vitamins.approval.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record AddApprovalDocumentRequest(
        @Schema(description = "업로드 완료된 파일 버전 구분 번호", example = "10")
        Long fileVersionId
) {
}

package com.group3.vitamins.approval.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record RejectApprovalLineRequest(
        @Schema(description = "반려 의견", example = "내용 보완이 필요합니다.")
        String opinion
) {
}

package com.group3.vitamins.approval.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApproveApprovalLineRequest(
        @Schema(description = "승인 의견", example = "확인했습니다.")
        String opinion
) {
}

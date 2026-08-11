package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalLineProcessResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ApprovalLineRejectResponse(
        @Schema(description = "결재선 구분 번호", example = "201")
        Long lineId,

        @Schema(description = "결재 단계 상태", example = "REJECTED")
        String status,

        @Schema(description = "처리 일시", example = "2026-08-02T10:00:00")
        LocalDateTime processedAt
) {

    public static ApprovalLineRejectResponse from(ApprovalLineProcessResult result) {
        return new ApprovalLineRejectResponse(result.lineId(), result.status(), result.processedAt());
    }
}

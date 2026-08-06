package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalLineProcessResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ApprovalLineProcessResponse(
        @Schema(description = "결재선 구분 번호", example = "202")
        Long lineId,

        @Schema(description = "결재 단계 상태", example = "APPROVED")
        String status,

        @Schema(description = "처리 일시", example = "2026-08-02T10:00:00")
        LocalDateTime processedAt,

        @Schema(description = "다음 활성화된 결재선 구분 번호(없으면 null)", example = "null")
        Long nextActiveLineId,

        @Schema(description = "이 처리로 전체 결재가 종료됐는지 여부", example = "true")
        boolean approvalCompleted
) {

    public static ApprovalLineProcessResponse from(ApprovalLineProcessResult result) {
        return new ApprovalLineProcessResponse(result.lineId(), result.status(), result.processedAt(),
                result.nextActiveLineId(), result.approvalCompleted());
    }
}

package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalSubmissionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SubmitApprovalResponse(
        @Schema(description = "결재 구분 번호", example = "1")
        Long approvalId,

        @Schema(description = "상신 회차 구분 번호", example = "1")
        Long revisionId,

        @Schema(description = "상신 회차 번호", example = "1")
        int revisionNo,

        @Schema(description = "회차 상태(상신 성공 시 IN_PROGRESS)", example = "IN_PROGRESS")
        String status,

        @Schema(description = "상신 일시", example = "2026-08-04T13:00:00")
        LocalDateTime submittedAt,

        @Schema(description = "1번 결재선(ACTIVE로 전환된) 구분 번호", example = "3")
        Long firstActiveLineId
) {

    public static SubmitApprovalResponse from(ApprovalSubmissionResult result) {
        return new SubmitApprovalResponse(
                result.approvalId(), result.revisionId(), result.revisionNo(),
                result.status().name(), result.submittedAt(), result.firstActiveLineId());
    }
}

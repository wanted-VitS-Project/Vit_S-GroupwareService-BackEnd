package com.group3.vitamins.approval.application.result;

import com.group3.vitamins.approval.domain.model.ApprovalStatus;

import java.time.LocalDateTime;

/** SUB-002 — 상신 결과 */
public record ApprovalSubmissionResult(
        Long approvalId,
        Long revisionId,
        int revisionNo,
        ApprovalStatus status,
        LocalDateTime submittedAt,
        Long firstActiveLineId
) {
}

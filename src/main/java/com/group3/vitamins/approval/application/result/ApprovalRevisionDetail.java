package com.group3.vitamins.approval.application.result;

import java.time.LocalDateTime;
import java.util.List;

/** MGT-005 — 결재 회차 상세조회 결과 */
public record ApprovalRevisionDetail(
        Long revisionId,
        int revisionNo,
        String title,
        String content,
        String drafterId,
        String drafterName,
        String drafterDepartment,
        String drafterPosition,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime finishedAt,
        List<ApprovalDocumentView> documents,
        List<ApprovalLineDetailView> lines
) {
}

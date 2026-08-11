package com.group3.vitamins.approval.infrastructure.persistence.row;

import java.time.LocalDateTime;

/**
 * 결재관리 목록조회(MGT-001~004) 1행 — {@code approval}+현재 회차({@code approval_revision})
 * +{@code block}/{@code step}/{@code project}+기안자·현재 결재자({@code employee}) 조인 결과.
 */
public record ApprovalListRow(
        Long approvalId,
        String title,
        String status,
        int currentRevisionNo,
        Long currentRevisionId,
        String drafterId,
        String drafterName,
        String currentApproverId,
        String currentApproverName,
        Long projectId,
        String projectName,
        Long stepId,
        String stepName,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime completedAt
) {
}

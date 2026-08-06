package com.group3.vitamins.approval.application.result;

import java.time.LocalDateTime;
import java.util.List;

/** 결재관리 목록조회(MGT-001~004) 1건 — 현재 회차 기준으로 조립한 요약 정보. */
public record ApprovalListItemResult(
        Long approvalId,
        String title,
        String status,
        int currentRevisionNo,
        String drafterId,
        String drafterName,
        String currentApproverId,
        String currentApproverName,
        Long projectId,
        String projectName,
        Long stepId,
        String stepName,
        List<ApprovalLinePreviewResult> lines,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime completedAt
) {
}

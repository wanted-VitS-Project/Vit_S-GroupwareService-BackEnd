package com.group3.vitamins.approval.application.result;

import java.time.LocalDateTime;

/** 결재선 1건 + 라이브 조회한 결재자 정보 — 회차 상세조회(MGT-005) 응답 조립용 */
public record ApprovalLineDetailView(
        Long lineId,
        String approverId,
        String approverName,
        String approverPosition,
        String approverDepartment,
        int order,
        String status,
        String opinion,
        LocalDateTime processedAt,
        boolean approverUnavailable
) {
}

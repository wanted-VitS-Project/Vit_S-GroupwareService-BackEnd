package com.group3.vitamins.approval.application.result;

/** 결재선 1건 + 라이브 조회한 결재자 정보(INV-11) — 응답 조립용 */
public record ApprovalLineView(
        Long lineId,
        String approverId,
        int order,
        String approverName,
        String approverPosition,
        String approverDepartment
) {
}

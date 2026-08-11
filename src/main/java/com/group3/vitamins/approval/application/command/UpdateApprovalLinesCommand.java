package com.group3.vitamins.approval.application.command;

import java.util.List;

/** APR-009 — 기존 결재선 전체를 이 목록으로 치환한다 */
public record UpdateApprovalLinesCommand(
        Long approvalId,
        Long revisionId,
        String requesterId,
        List<LineInput> lines
) {
    public record LineInput(String approverId, int order) {
    }
}

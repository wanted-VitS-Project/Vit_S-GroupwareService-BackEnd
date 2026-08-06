package com.group3.vitamins.approval.application.command;

/** 결재 반려(PRC-005~009). */
public record RejectApprovalLineCommand(Long lineId, String opinion, String requesterId) {
}

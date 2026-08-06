package com.group3.vitamins.approval.application.command;

/** 결재 승인(PRC-001~004). */
public record ApproveApprovalLineCommand(Long lineId, String opinion, String requesterId) {
}

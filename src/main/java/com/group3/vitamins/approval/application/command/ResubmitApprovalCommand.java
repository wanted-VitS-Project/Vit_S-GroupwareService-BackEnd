package com.group3.vitamins.approval.application.command;

public record ResubmitApprovalCommand(Long approvalId, String requesterId) {
}

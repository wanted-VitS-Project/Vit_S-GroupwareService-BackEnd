package com.group3.vitamins.approval.application.command;

public record SubmitApprovalCommand(Long approvalId, Long revisionId, String requesterId) {
}

package com.group3.vitamins.approval.application.command;

public record AddApprovalDocumentCommand(Long approvalId, Long revisionId, Long fileVersionId, String requesterId) {
}

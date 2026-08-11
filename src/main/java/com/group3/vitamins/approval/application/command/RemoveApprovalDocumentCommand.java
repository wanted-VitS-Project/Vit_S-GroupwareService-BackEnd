package com.group3.vitamins.approval.application.command;

public record RemoveApprovalDocumentCommand(Long approvalId, Long revisionId, Long documentId, String requesterId) {
}

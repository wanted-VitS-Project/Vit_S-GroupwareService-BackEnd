package com.group3.vitamins.approval.application.query;

public record GetApprovalRevisionQuery(Long approvalId, Long revisionId, String requesterId) {
}

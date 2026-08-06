package com.group3.vitamins.approval.application.query;

/** 결재 이력조회(MGT-007). */
public record GetApprovalHistoryQuery(Long approvalId, String requesterId) {
}

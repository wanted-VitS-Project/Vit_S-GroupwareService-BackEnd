package com.group3.vitamins.approval.application.query;

/** 결재 상세조회(MGT-005~006) — 항상 현재 회차를 대상으로 한다(회차 지정 불가). */
public record GetApprovalDetailQuery(Long approvalId, String requesterId) {
}

package com.group3.vitamins.approval.domain.model;

/** APR-009 — 결재선 등록·수정 요청 1건(치환 대상). 검증 통과 후 저장 입력으로 쓴다 */
public record NewApprovalLine(String approverId, int sequenceNo) {
}

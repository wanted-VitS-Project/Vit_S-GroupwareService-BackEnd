package com.group3.vitamins.approval.infrastructure.persistence.row;

/** 결재관리 목록조회의 결재선 미리보기 배치 조회 1행 — {@code revisionId}로 상위 행에 묶는다. */
public record ApprovalLinePreviewRow(Long revisionId, String approverId, String approverName, int sequenceNo, String status) {
}

package com.group3.vitamins.approval.infrastructure.blockdetail;

/** 블록 미리보기용 결재선 상태 조회 행. 회차 하나당 여러 행이 온다. */
public record ApprovalLineRow(Long revisionId, String status) {
}

package com.group3.vitamins.approval.infrastructure.blockdetail;

/** 블록 미리보기용 최신 회차 조회 행. approval 하나당 1행(최신 revision_no 기준). */
public record ApprovalRevisionRow(Long approvalId, Long revisionId, int revisionNo, String status, String title, String content) {
}

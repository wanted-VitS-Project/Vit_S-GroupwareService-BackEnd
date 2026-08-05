package com.group3.vitamins.project.block.application.result;

/**
 * APPROVAL 블록 상세(카드 미리보기용, BND-003). 결재 자체의 조회 API(`.ai/api/approval.md`)와는
 * 별개로, 블록 목록 조회 화면에서 진행 현황만 가볍게 보여줄 때 쓴다.
 */
public record ApprovalDetail(Long approvalId, String status, int totalLines, int approvedLines)
        implements BlockDetail {
}

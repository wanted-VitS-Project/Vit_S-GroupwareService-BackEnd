package com.group3.vitamins.project.block.application.result;

/**
 * APPROVAL 블록 상세(카드 미리보기용, BND-003). 결재 자체의 조회 API(`.ai/api/approval.md`)와는
 * 별개로, 블록 목록 조회 화면에서 진행 현황만 가볍게 보여줄 때 쓴다.
 *
 * <p>{@code revisionId}는 여기가 유일하게 노출하는 곳이다 — `#88` 삭제 이후 결재 관련 다른 어떤
 * 응답도 회차 PK를 내려주지 않아서, 프론트가 결재 상세 조회 등(`.../revisions/{revisionId}/...`)을
 * 부르려면 반드시 이 블록 목록 조회를 먼저 거쳐야 한다.
 */
public record ApprovalDetail(Long approvalId, Long revisionId, int revisionNo,
                             String status, int totalLines, int approvedLines)
        implements BlockDetail {
}

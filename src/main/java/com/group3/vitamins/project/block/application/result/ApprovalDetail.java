package com.group3.vitamins.project.block.application.result;

/**
 * APPROVAL 블록 상세(카드 미리보기용, BND-003). 결재 자체의 조회 API(`.ai/api/approval.md`)와는
 * 별개로, 블록 목록 조회 화면에서 진행 현황만 가볍게 보여줄 때 쓴다.
 *
 * <p>{@code revisionId}는 여기가 유일하게 노출하는 곳이다 — `#88` 삭제 이후 결재 관련 다른 어떤
 * 응답도 회차 PK를 내려주지 않아서, 프론트가 결재 상세 조회 등(`.../revisions/{revisionId}/...`)을
 * 부르려면 반드시 이 블록 목록 조회를 먼저 거쳐야 한다.
 *
 * <p>⚠️ {@code status}는 {@code approval.status}가 아니라 **최신 {@code approval_revision.status}**다
 * (`ApprovalDetailMapper.findLatestRevisions`). 의도적인 선택이다 — 반려 후 "수정"으로 재상신 회차를
 * 만들면 {@code approval.status}는 상신 전까지 여전히 {@code REJECTED}로 남지만(멱등 처리 때문), 이
 * 필드는 새로 만든 회차를 따라가서 즉시 {@code DRAFT}로 바뀐다. 그래서 프론트는 이 값 하나로 "반려 배너를
 * 보여줄지"와 "상신 버튼을 활성화할지"를 동시에 판단할 수 있다 — {@code DRAFT}면 이미 재상신 준비가 끝난
 * 것이라 상신을 눌러도 된다.
 *
 * <p>{@code title}/{@code content}는 최신 회차의 실제 제목·내용 값이다 — 카드에 "결재 제목" 글씨
 * 바로 아래 "결재 내용"이 이어서 보이는 자리가 이 두 필드다. 길이 제한 없이 그대로 내려주고, 카드
 * 영역에 맞춰 자르는 건 프론트 표시 로직(예: CSS 줄바꿈 제한)의 몫이다.
 */
public record ApprovalDetail(Long approvalId, Long revisionId, int revisionNo,
                             String status, String title, String content, int totalLines, int approvedLines,
                             boolean requiresApproverReplacement)
        implements BlockDetail {
}

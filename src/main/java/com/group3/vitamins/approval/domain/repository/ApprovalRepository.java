package com.group3.vitamins.approval.domain.repository;

import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalWithRevision;
import com.group3.vitamins.approval.domain.model.NewApprovalLine;

import java.util.List;
import java.util.Optional;

/** 결재 도메인이 바라보는 영속성 포트. 구현체는 infrastructure/catalog 에 있다 (`text.domain.repository.TextRepository`와 동일 구조). */
public interface ApprovalRepository {

    /** APR-001 — 1회차 DRAFT 로 {@code approval}+{@code approval_revision} 을 함께 생성한다 */
    ApprovalWithRevision createDraft(Long blockId, String drafterId);

    Optional<Approval> findApproval(Long approvalId);

    Optional<ApprovalRevision> findRevisionById(Long revisionId);

    /** SUB-005~008 — 이 결재의 가장 최신 회차(재상신 대상 판단·멱등 확인용) */
    Optional<ApprovalRevision> findLatestRevision(Long approvalId);

    List<ApprovalLine> findLinesByRevisionId(Long revisionId);

    List<ApprovalDocument> findDocumentsByRevisionId(Long revisionId);

    /** APR-002 — DRAFT 상태일 때만 제목·내용을 갱신한다. DRAFT 가 아니면 409 를 던진다 */
    ApprovalRevision updateDraftContent(Long revisionId, String title, String content);

    /** APR-009 — 기존 결재선을 전부 지우고({@code sequenceNo} 순으로) 새로 만든다 */
    List<ApprovalLine> replaceLines(Long revisionId, List<NewApprovalLine> lines);

    /** SUB-006 — 이전 회차 제목·내용을 그대로 넘긴 새 DRAFT 회차 */
    ApprovalRevision createRevisionDraft(Long approvalId, int revisionNo, String title, String content);

    /** SUB-006 — 이전 회차 문서를 새 회차로 복사한다 */
    List<ApprovalDocument> copyDocuments(Long newRevisionId, List<Long> fileVersionIds);
}

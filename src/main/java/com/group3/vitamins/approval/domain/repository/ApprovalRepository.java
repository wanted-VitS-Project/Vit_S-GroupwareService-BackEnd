package com.group3.vitamins.approval.domain.repository;

import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalWithRevision;
import com.group3.vitamins.approval.domain.model.NewApprovalLine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 결재 도메인이 바라보는 영속성 포트. 구현체는 infrastructure/catalog 에 있다 (`text.domain.repository.TextRepository`와 동일 구조). */
public interface ApprovalRepository {

    /** APR-001 — 1회차 DRAFT 로 {@code approval}+{@code approval_revision} 을 함께 생성한다 */
    ApprovalWithRevision createDraft(Long blockId, String drafterId);

    Optional<Approval> findApproval(Long approvalId);

    Optional<ApprovalRevision> findRevisionById(Long revisionId);

    /** 결재선 전체 치환(APR-009) 직전 상태 재확인용 잠금 조회 — 상신(동시 상태 변경)과의 레이스 방지 */
    Optional<ApprovalRevision> findRevisionByIdForUpdate(Long revisionId);

    /** SUB-005~008 — 이 결재의 가장 최신 회차(재상신 대상 판단·멱등 확인용). 비관적 락 있음(쓰기 경로 전용) */
    Optional<ApprovalRevision> findLatestRevision(Long approvalId);

    /**
     * {@link #findLatestRevision}과 조건은 같지만 잠금이 없다. 블록 카드 미리보기처럼
     * 읽기 전용 트랜잭션에서 최신 회차만 가볍게 조회할 때 쓴다(락 걸린 쿼리를 읽기 전용
     * 트랜잭션에서 호출하면 DB가 거부한다).
     */
    Optional<ApprovalRevision> findLatestRevisionReadOnly(Long approvalId);

    List<ApprovalLine> findLinesByRevisionId(Long revisionId);

    List<ApprovalDocument> findDocumentsByRevisionId(Long revisionId);

    Optional<ApprovalDocument> findDocumentById(Long documentId);

    /** APR-006 — 동일 회차에 동일 file_version_id 가 이미 연결돼 있는지(DB UNIQUE 대신 애플리케이션 검증) */
    boolean existsDocument(Long revisionId, Long fileVersionId);

    /** APR-005 — 문서 1건 연결 */
    ApprovalDocument addDocument(Long revisionId, Long fileVersionId);

    /** APR-007 — 하드 삭제(이력 보존 대상 아님) */
    void deleteDocument(Long documentId);

    /** APR-002 — DRAFT 상태일 때만 제목·내용을 갱신한다. DRAFT 가 아니면 409 를 던진다 */
    ApprovalRevision updateDraftContent(Long revisionId, String title, String content);

    /** APR-009 — 기존 결재선을 전부 지우고({@code sequenceNo} 순으로) 새로 만든다 */
    List<ApprovalLine> replaceLines(Long revisionId, List<NewApprovalLine> lines);

    /** SUB-006 — 이전 회차 제목·내용을 그대로 넘긴 새 DRAFT 회차 */
    ApprovalRevision createRevisionDraft(Long approvalId, int revisionNo, String title, String content);

    /** SUB-006 — 이전 회차 문서를 새 회차로 복사한다 */
    List<ApprovalDocument> copyDocuments(Long newRevisionId, List<Long> fileVersionIds);

    /**
     * SUB-002 — 검증 통과 후 상태 전이. 호출 전에 {@code findRevisionByIdForUpdate} 로 이미 잠금·DRAFT
     * 확인이 끝난 상태라고 가정한다(INV-07) — 여기서 별도 조건부 UPDATE 를 다시 걸지 않는다.
     */
    ApprovalRevision markRevisionSubmitted(Long revisionId);

    /** SUB-002 — approval 을 IN_PROGRESS 로 전환하고 {@code current_revision_no} 를 이 회차로 갱신한다 */
    void markApprovalInProgress(Long approvalId, int revisionNo);

    /** SUB-002 — 1번 결재선은 ACTIVE, 나머지는 WAITING 으로 전환한 뒤 순서대로 다시 읽어 반환한다 */
    List<ApprovalLine> activateLines(Long revisionId);

    /**
     * 블록 삭제(`ApprovalBlockDetailAdapter.deleteDetail`) 시 호출 — {@code approval}과 그 아래
     * 모든 {@code approval_revision}·{@code approval_line}을 논리 삭제한다. 호출 전에 이미
     * {@code IN_PROGRESS}가 아님이 확인된 상태라고 가정한다. {@code approval_document}는 애초에
     * 하드 삭제 전용(APR-007)이라 {@code deleted_at}을 안 쓰므로 여기서 건드리지 않는다.
     */
    void softDeleteCascade(Long approvalId, LocalDateTime deletedAt);
}

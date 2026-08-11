package com.group3.vitamins.approval.domain.repository;

import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
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

    /** DEL-006 — 쓰기 진입점이 가장 먼저 잠그는 활성 결재 부모 행 */
    Optional<Approval> findApprovalForUpdate(Long approvalId);

    /** DEL-013 — 삭제 전파가 이미 삭제된 행까지 잠근 뒤 멱등 종료할 수 있게 하는 조회 */
    Optional<Approval> findApprovalIncludingDeletedForUpdate(Long approvalId);

    /** 원 기안자 참여 불가 시 현재 스텝 EDITOR를 대행 기안자로 지정한다. 부모 행 잠금 뒤 호출한다. */
    Approval assignActingDrafter(Long approvalId, String actingDrafterId);

    /** 승인·반려가 line 잠금 전에 부모 approval을 찾기 위한 활성 관계 조회 */
    Optional<Long> findApprovalIdByLineId(Long lineId);

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

    /** MGT-007 — 이력 조회 권한 판정용. 이 결재의 모든 회차를 통틀어 결재선을 전부 가져온다(삭제분 제외). */
    List<ApprovalLine> findLinesByApprovalId(Long approvalId);

    /** MGT-007 — 회차별 이력. {@code revisionNo} 오름차순 */
    List<ApprovalRevision> findRevisionsByApprovalId(Long approvalId);

    /** PRC-001 — 결재선 처리(승인·반려) 직전 잠금 조회. 동시 처리 방지(INV-07과 동일 이유) */
    Optional<ApprovalLine> findLineByIdForUpdate(Long lineId);

    /** PRC-002/PRC-007 — 결재자 처리(승인·반려) 반영. {@code status} 로 승인/반려를 함께 표현한다 */
    ApprovalLine markLineProcessed(Long lineId, ApprovalLineStatus status, String opinion);

    /** PRC-002 — 다음 순번 결재선(WAITING) 조회. 없으면 이 회차의 마지막 순번이었다는 뜻 */
    Optional<ApprovalLine> findLineBySequenceNo(Long revisionId, int sequenceNo);

    /** PRC-002 — 다음 결재선을 ACTIVE 로 전환 */
    ApprovalLine activateLine(Long lineId);

    /** PRC-002/PRC-007 — 마지막 결재선 처리 시 회차·결재 모두 최종 상태로 종료(단일 트랜잭션) */
    void finalizeApproval(Long approvalId, Long revisionId, ApprovalStatus finalStatus);

    /** PRC-007 — 반려 시 이후 순번의 {@code WAITING} 결재선을 전부 {@code CANCELED}로 전환 */
    void cancelWaitingLinesAfter(Long revisionId, int sequenceNo);

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

    /** 진행 중 참여 불가 결재선 1건을 감사 이력 보존 후 같은 순번·상태의 새 결재자로 교체한다. */
    ApprovalLine replaceUnavailableLine(ApprovalLine previousLine, String newApproverId);

    /** 진행 중 참여 불가 결재선을 제외하고 남은 활성 결재선의 순번을 1부터 다시 매긴다. */
    List<ApprovalLine> excludeUnavailableLines(Long revisionId, List<Long> excludedLineIds);

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

    /** DEL-002~005 — 미종결 상태를 CANCELED로 종결하고 결재 하위 행 전부를 논리 삭제한다. */
    void softDeleteCascade(Long approvalId, LocalDateTime deletedAt);
}

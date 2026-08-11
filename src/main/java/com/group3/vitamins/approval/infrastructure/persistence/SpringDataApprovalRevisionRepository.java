package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataApprovalRevisionRepository extends JpaRepository<ApprovalRevisionJpaEntity, Long> {

    /** MGT-007 — 이력 조회. 회차 번호 오름차순 */
    List<ApprovalRevisionJpaEntity> findByApprovalIdAndDeletedAtIsNullOrderByRevisionNoAsc(Long approvalId);

    /**
     * SUB-005~008 — 재상신 대상(REJECTED)인지, 이미 준비된 DRAFT 회차가 있는지 판단하는 데 쓰는 최신 회차.
     *
     * <p>{@code @Lock(PESSIMISTIC_WRITE)} — 동시에 재상신 호출 2건이 들어오면 둘 다 "아직 REJECTED"로
     * 읽고 각자 새 회차를 insert 시도해 {@code UNIQUE(approval_id, revision_no)} 충돌로 하나가 500 에러가
     * 난다(`AccountJpaRepository.findByUserIdForUpdate`와 동일한 이유의 락). 뒤에 대기한 트랜잭션은 락이
     * 풀린 뒤 방금 만들어진 DRAFT 회차를 다시 읽어 SUB-008 멱등 경로로 빠진다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ApprovalRevisionJpaEntity> findTopByApprovalIdAndDeletedAtIsNullOrderByRevisionNoDesc(Long approvalId);

    /**
     * 위와 동일한 조건(최신 회차)이지만 잠금이 없는 순수 조회 버전. 블록 카드 미리보기
     * (`ApprovalBlockDetailAdapter.loadDetails`)처럼 읽기 전용 트랜잭션에서 호출해야 하는 경우 쓴다 —
     * `@Lock(PESSIMISTIC_WRITE)`는 `SELECT ... FOR UPDATE`라 읽기 전용 트랜잭션에서 실행하면 DB가 거부한다.
     */
    Optional<ApprovalRevisionJpaEntity> findFirstByApprovalIdAndDeletedAtIsNullOrderByRevisionNoDesc(Long approvalId);

    Optional<ApprovalRevisionJpaEntity> findByApprovalRevisionIdAndDeletedAtIsNull(Long approvalRevisionId);

    /** {@code updateLines}(APR-009)가 결재선 치환 직전 회차 상태를 잠금 조회로 재확인할 때 쓴다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ApprovalRevisionJpaEntity r WHERE r.approvalRevisionId = :revisionId "
            + "AND r.deletedAt IS NULL")
    Optional<ApprovalRevisionJpaEntity> findByIdForUpdate(@Param("revisionId") Long revisionId);

    /**
     * DRAFT 조건을 UPDATE 문 자체에 걸어 "확인 후 쓰기" 사이의 틈을 없앤다(`text.SpringDataTextRepository`와 동일 이유).
     * 그 틈에 상신(#7)돼 DRAFT 를 벗어났으면 0건 갱신되고, 그걸 409로 처리한다.
     * 벌크 UPDATE 는 {@code @UpdateTimestamp} 를 안 타므로 {@code updatedAt} 을 직접 SET 한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalRevisionJpaEntity r SET r.title = :title, r.content = :content, "
            + "r.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE r.approvalRevisionId = :revisionId AND r.status = :draftStatus AND r.deletedAt IS NULL")
    int updateTitleContentIfDraft(@Param("revisionId") Long revisionId,
                                   @Param("title") String title,
                                   @Param("content") String content,
                                   @Param("draftStatus") ApprovalStatus draftStatus);

    /**
     * SUB-002 — 상신. 호출 전에 {@code findByIdForUpdate} 로 이미 잠금·DRAFT 확인이 끝났다고 가정하고
     * 조건 없이 전환한다(INV-07 — 락으로 이미 레이스를 막았으니 여기서 또 걸 필요 없음).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalRevisionJpaEntity r SET r.status = :inProgress, r.submittedAt = CURRENT_TIMESTAMP, "
            + "r.updatedAt = CURRENT_TIMESTAMP WHERE r.approvalRevisionId = :revisionId AND r.deletedAt IS NULL")
    void markSubmitted(@Param("revisionId") Long revisionId, @Param("inProgress") ApprovalStatus inProgress);

    /** PRC-002/PRC-007 — 마지막 결재선 처리 시 회차를 최종 상태로 종료(`COMPLETED`/`REJECTED`) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalRevisionJpaEntity r SET r.status = :finalStatus, r.finishedAt = CURRENT_TIMESTAMP, "
            + "r.updatedAt = CURRENT_TIMESTAMP WHERE r.approvalRevisionId = :revisionId AND r.deletedAt IS NULL")
    void finalizeRevision(@Param("revisionId") Long revisionId, @Param("finalStatus") ApprovalStatus finalStatus);

    /** 블록 삭제(`ApprovalBlockDetailAdapter.deleteDetail`) — 이 결재의 회차 전부를 논리 삭제한다 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalRevisionJpaEntity r SET r.status = CASE WHEN r.status IN :cancelableStatuses "
            + "THEN :canceled ELSE r.status END, r.deletedAt = :deletedAt, r.updatedAt = :deletedAt "
            + "WHERE r.approvalId = :approvalId AND r.deletedAt IS NULL")
    void softDeleteByApprovalId(@Param("approvalId") Long approvalId,
                                @Param("deletedAt") LocalDateTime deletedAt,
                                @Param("cancelableStatuses") Collection<ApprovalStatus> cancelableStatuses,
                                @Param("canceled") ApprovalStatus canceled);
}

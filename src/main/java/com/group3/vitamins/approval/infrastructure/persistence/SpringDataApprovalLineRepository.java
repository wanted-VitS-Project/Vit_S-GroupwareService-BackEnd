package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
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

public interface SpringDataApprovalLineRepository extends JpaRepository<ApprovalLineJpaEntity, Long> {

    List<ApprovalLineJpaEntity> findByApprovalRevisionIdAndDeletedAtIsNullOrderBySequenceNo(Long approvalRevisionId);

    Optional<ApprovalLineJpaEntity> findByApprovalLineIdAndDeletedAtIsNull(Long approvalLineId);

    /** PRC-001 — 결재선 처리(승인·반려) 직전 잠금 조회. 동시 요청·이중 클릭 방지(INV-07과 동일 이유) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ApprovalLineJpaEntity l WHERE l.approvalLineId = :lineId AND l.deletedAt IS NULL")
    Optional<ApprovalLineJpaEntity> findByIdForUpdate(@Param("lineId") Long lineId);

    /** PRC-002/PRC-007 — 승인·반려 반영. 잠금 조회로 이미 ACTIVE 확인이 끝난 뒤 호출된다고 가정(INV-07) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.status = :status, l.opinion = :opinion, "
            + "l.processedAt = CURRENT_TIMESTAMP WHERE l.approvalLineId = :lineId AND l.deletedAt IS NULL")
    void markProcessed(@Param("lineId") Long lineId, @Param("status") ApprovalLineStatus status,
                        @Param("opinion") String opinion);

    /** PRC-002 — 다음 순번 결재선(WAITING) 조회. 없으면 이 회차의 마지막 순번이었다는 뜻 */
    Optional<ApprovalLineJpaEntity> findByApprovalRevisionIdAndSequenceNoAndDeletedAtIsNull(
            Long approvalRevisionId, int sequenceNo);

    /** PRC-002 — 다음 결재선을 ACTIVE 로 전환 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.status = :active WHERE l.approvalLineId = :lineId "
            + "AND l.deletedAt IS NULL")
    void activate(@Param("lineId") Long lineId, @Param("active") ApprovalLineStatus active);

    /** PRC-007 — 반려 시 이후 순번의 WAITING 결재선을 전부 CANCELED 로 전환 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.status = :canceled WHERE l.approvalRevisionId = :revisionId "
            + "AND l.sequenceNo > :sequenceNo AND l.status = :waiting AND l.deletedAt IS NULL")
    void cancelWaitingAfter(@Param("revisionId") Long revisionId, @Param("sequenceNo") int sequenceNo,
                             @Param("waiting") ApprovalLineStatus waiting, @Param("canceled") ApprovalLineStatus canceled);

    /** MGT-007 — 이력 조회 권한 판정용. 이 결재의 모든 회차에 속한 결재선을 전부 가져온다(삭제분 제외). */
    @Query("SELECT l FROM ApprovalLineJpaEntity l WHERE l.approvalRevisionId IN "
            + "(SELECT r.approvalRevisionId FROM ApprovalRevisionJpaEntity r WHERE r.approvalId = :approvalId "
            + "AND r.deletedAt IS NULL) "
            + "AND l.deletedAt IS NULL")
    List<ApprovalLineJpaEntity> findByApprovalId(@Param("approvalId") Long approvalId);

    /**
     * APR-009 전체 치환의 삭제 단계 — 논리 삭제다(`DELETE.md` D-1·D-2).
     *
     * <p>하드 삭제였다가 전환했다. {@code approval_line}은 하드 대상인 「연결 행 7종」이 아니고
     * 의견·처리시각이 담긴 실물이라 D-1이 적용된다. 치환된 이전 결재선은 행으로 남고, 활성 조회는
     * {@code deletedAt IS NULL}로 새 결재선만 본다. 같은 순번을 다시 넣을 수 있게 UNIQUE 를 낮춘
     * 마이그레이션(`V20260811161100`)이 선행 조건이다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.deletedAt = CURRENT_TIMESTAMP "
            + "WHERE l.approvalRevisionId = :approvalRevisionId AND l.deletedAt IS NULL")
    void softDeleteAllByApprovalRevisionId(@Param("approvalRevisionId") Long approvalRevisionId);

    /** 참여 불가 결재자 교체 전 기존 미처리 행을 CANCELED로 종결하고 논리 삭제한다. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.status = :canceled, l.deletedAt = CURRENT_TIMESTAMP "
            + "WHERE l.approvalLineId = :lineId AND l.deletedAt IS NULL")
    void cancelAndSoftDelete(@Param("lineId") Long lineId,
                             @Param("canceled") ApprovalLineStatus canceled);

    /** SUB-002— 1번 순번은 ACTIVE, 나머지는 WAITING (회차 잠금이 이미 걸려 있어 조건 없이 전환) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.status = CASE WHEN l.sequenceNo = 1 THEN :activeStatus ELSE :waitingStatus END "
            + "WHERE l.approvalRevisionId = :revisionId AND l.deletedAt IS NULL")
    void activateFirstAndWaitRest(@Param("revisionId") Long revisionId,
                                   @Param("activeStatus") ApprovalLineStatus activeStatus,
                                   @Param("waitingStatus") ApprovalLineStatus waitingStatus);

    /**
     * 블록 삭제(`ApprovalBlockDetailAdapter.deleteDetail`) — 이 결재의 모든 회차에 속한 결재선을
     * 논리 삭제한다. {@code approval_line}은 {@code approval_revision_id}만 갖고 있어 서브쿼리로 묶는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.status = CASE WHEN l.status IN :cancelableStatuses "
            + "THEN :canceled ELSE l.status END, l.deletedAt = :deletedAt "
            + "WHERE l.approvalRevisionId IN "
            + "(SELECT r.approvalRevisionId FROM ApprovalRevisionJpaEntity r WHERE r.approvalId = :approvalId) "
            + "AND l.deletedAt IS NULL")
    void softDeleteByApprovalId(@Param("approvalId") Long approvalId,
                                @Param("deletedAt") LocalDateTime deletedAt,
                                @Param("cancelableStatuses") Collection<ApprovalLineStatus> cancelableStatuses,
                                @Param("canceled") ApprovalLineStatus canceled);

    @Query("SELECT a.approvalId FROM ApprovalLineJpaEntity l, ApprovalRevisionJpaEntity r, ApprovalJpaEntity a "
            + "WHERE l.approvalLineId = :lineId AND l.approvalRevisionId = r.approvalRevisionId "
            + "AND r.approvalId = a.approvalId AND l.deletedAt IS NULL AND r.deletedAt IS NULL "
            + "AND a.deletedAt IS NULL")
    Optional<Long> findActiveApprovalIdByLineId(@Param("lineId") Long lineId);
}

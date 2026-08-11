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
import java.util.Optional;

public interface SpringDataApprovalRepository extends JpaRepository<ApprovalJpaEntity, Long> {

    Optional<ApprovalJpaEntity> findByApprovalIdAndDeletedAtIsNull(Long approvalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ApprovalJpaEntity a WHERE a.approvalId = :approvalId AND a.deletedAt IS NULL")
    Optional<ApprovalJpaEntity> findActiveByIdForUpdate(@Param("approvalId") Long approvalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ApprovalJpaEntity a WHERE a.approvalId = :approvalId")
    Optional<ApprovalJpaEntity> findIncludingDeletedByIdForUpdate(@Param("approvalId") Long approvalId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalJpaEntity a SET a.actingDrafterId = :actingDrafterId, a.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE a.approvalId = :approvalId AND a.deletedAt IS NULL")
    void assignActingDrafter(@Param("approvalId") Long approvalId,
                             @Param("actingDrafterId") String actingDrafterId);

    /**
     * SUB-002 — 상신 시 approval 을 IN_PROGRESS 로, {@code current_revision_no} 를 이 회차로 갱신한다.
     * 회차 잠금(`findByIdForUpdate`)이 이미 이 트랜잭션 안에서 걸려 있어 별도 조건은 안 건다(INV-07).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalJpaEntity a SET a.status = :inProgress, a.currentRevisionNo = :revisionNo, "
            + "a.updatedAt = CURRENT_TIMESTAMP WHERE a.approvalId = :approvalId AND a.deletedAt IS NULL")
    void markInProgress(@Param("approvalId") Long approvalId,
                         @Param("revisionNo") int revisionNo,
                         @Param("inProgress") ApprovalStatus inProgress);

    /** PRC-002/PRC-007 — 마지막 결재선 처리 시 approval을 최종 상태로 종료(`COMPLETED`/`REJECTED`) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalJpaEntity a SET a.status = :finalStatus, a.completedAt = CURRENT_TIMESTAMP, "
            + "a.updatedAt = CURRENT_TIMESTAMP WHERE a.approvalId = :approvalId AND a.deletedAt IS NULL")
    void finalizeApproval(@Param("approvalId") Long approvalId, @Param("finalStatus") ApprovalStatus finalStatus);

    /** 블록 삭제(`ApprovalBlockDetailAdapter.deleteDetail`) — approval 자체를 논리 삭제한다 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalJpaEntity a SET a.status = CASE WHEN a.status IN :cancelableStatuses "
            + "THEN :canceled ELSE a.status END, a.deletedAt = :deletedAt, a.updatedAt = :deletedAt "
            + "WHERE a.approvalId = :approvalId AND a.deletedAt IS NULL")
    void softDelete(@Param("approvalId") Long approvalId,
                    @Param("deletedAt") LocalDateTime deletedAt,
                    @Param("cancelableStatuses") Collection<ApprovalStatus> cancelableStatuses,
                    @Param("canceled") ApprovalStatus canceled);
}

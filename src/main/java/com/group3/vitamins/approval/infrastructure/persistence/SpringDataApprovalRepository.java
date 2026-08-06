package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SpringDataApprovalRepository extends JpaRepository<ApprovalJpaEntity, Long> {

    /**
     * SUB-002 — 상신 시 approval 을 IN_PROGRESS 로, {@code current_revision_no} 를 이 회차로 갱신한다.
     * 회차 잠금(`findByIdForUpdate`)이 이미 이 트랜잭션 안에서 걸려 있어 별도 조건은 안 건다(INV-07).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalJpaEntity a SET a.status = :inProgress, a.currentRevisionNo = :revisionNo, "
            + "a.updatedAt = CURRENT_TIMESTAMP WHERE a.approvalId = :approvalId")
    void markInProgress(@Param("approvalId") Long approvalId,
                         @Param("revisionNo") int revisionNo,
                         @Param("inProgress") ApprovalStatus inProgress);

    /** PRC-002/PRC-007 — 마지막 결재선 처리 시 approval을 최종 상태로 종료(`COMPLETED`/`REJECTED`) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalJpaEntity a SET a.status = :finalStatus, a.completedAt = CURRENT_TIMESTAMP, "
            + "a.updatedAt = CURRENT_TIMESTAMP WHERE a.approvalId = :approvalId")
    void finalizeApproval(@Param("approvalId") Long approvalId, @Param("finalStatus") ApprovalStatus finalStatus);

    /** 블록 삭제(`ApprovalBlockDetailAdapter.deleteDetail`) — approval 자체를 논리 삭제한다 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalJpaEntity a SET a.deletedAt = :deletedAt, a.updatedAt = :deletedAt "
            + "WHERE a.approvalId = :approvalId")
    void softDelete(@Param("approvalId") Long approvalId, @Param("deletedAt") LocalDateTime deletedAt);
}

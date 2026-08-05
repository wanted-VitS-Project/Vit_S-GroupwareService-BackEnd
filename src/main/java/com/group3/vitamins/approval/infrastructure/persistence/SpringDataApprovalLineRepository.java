package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataApprovalLineRepository extends JpaRepository<ApprovalLineJpaEntity, Long> {

    List<ApprovalLineJpaEntity> findByApprovalRevisionIdOrderBySequenceNo(Long approvalRevisionId);

    /** APR-009 전체 치환의 삭제 단계 — 하드 삭제(DRAFT 편집은 이력 보존 대상 아님, APR-007과 같은 논리) */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ApprovalLineJpaEntity l WHERE l.approvalRevisionId = :approvalRevisionId")
    void deleteAllByApprovalRevisionId(@Param("approvalRevisionId") Long approvalRevisionId);

    /** SUB-002 — 1번 순번은 ACTIVE, 나머지는 WAITING (회차 잠금이 이미 걸려 있어 조건 없이 전환) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.status = CASE WHEN l.sequenceNo = 1 THEN :activeStatus ELSE :waitingStatus END "
            + "WHERE l.approvalRevisionId = :revisionId")
    void activateFirstAndWaitRest(@Param("revisionId") Long revisionId,
                                   @Param("activeStatus") ApprovalLineStatus activeStatus,
                                   @Param("waitingStatus") ApprovalLineStatus waitingStatus);

    /**
     * 블록 삭제(`ApprovalBlockDetailAdapter.deleteDetail`) — 이 결재의 모든 회차에 속한 결재선을
     * 논리 삭제한다. {@code approval_line}은 {@code approval_revision_id}만 갖고 있어 서브쿼리로 묶는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalLineJpaEntity l SET l.deletedAt = :deletedAt "
            + "WHERE l.approvalRevisionId IN "
            + "(SELECT r.approvalRevisionId FROM ApprovalRevisionJpaEntity r WHERE r.approvalId = :approvalId)")
    void softDeleteByApprovalId(@Param("approvalId") Long approvalId, @Param("deletedAt") LocalDateTime deletedAt);
}

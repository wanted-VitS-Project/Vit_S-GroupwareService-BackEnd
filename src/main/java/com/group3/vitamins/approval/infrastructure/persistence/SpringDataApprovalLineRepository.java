package com.group3.vitamins.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataApprovalLineRepository extends JpaRepository<ApprovalLineJpaEntity, Long> {

    List<ApprovalLineJpaEntity> findByApprovalRevisionIdOrderBySequenceNo(Long approvalRevisionId);

    /** APR-009 전체 치환의 삭제 단계 — 하드 삭제(DRAFT 편집은 이력 보존 대상 아님, APR-007과 같은 논리) */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ApprovalLineJpaEntity l WHERE l.approvalRevisionId = :approvalRevisionId")
    void deleteAllByApprovalRevisionId(@Param("approvalRevisionId") Long approvalRevisionId);
}

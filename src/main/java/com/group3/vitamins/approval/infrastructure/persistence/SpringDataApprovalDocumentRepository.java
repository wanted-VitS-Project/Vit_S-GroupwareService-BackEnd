package com.group3.vitamins.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataApprovalDocumentRepository extends JpaRepository<ApprovalDocumentJpaEntity, Long> {

    List<ApprovalDocumentJpaEntity> findByApprovalRevisionId(Long approvalRevisionId);

    /** APR-006 중복 연결 확인 */
    boolean existsByApprovalRevisionIdAndFileVersionId(Long approvalRevisionId, Long fileVersionId);
}

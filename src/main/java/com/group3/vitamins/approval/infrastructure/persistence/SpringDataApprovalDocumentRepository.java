package com.group3.vitamins.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataApprovalDocumentRepository extends JpaRepository<ApprovalDocumentJpaEntity, Long> {

    List<ApprovalDocumentJpaEntity> findByApprovalRevisionIdAndDeletedAtIsNull(Long approvalRevisionId);
}

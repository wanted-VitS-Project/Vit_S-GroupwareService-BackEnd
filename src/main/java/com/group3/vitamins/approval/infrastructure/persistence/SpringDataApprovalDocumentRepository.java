package com.group3.vitamins.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataApprovalDocumentRepository extends JpaRepository<ApprovalDocumentJpaEntity, Long> {

    List<ApprovalDocumentJpaEntity> findByApprovalRevisionIdAndDeletedAtIsNull(Long approvalRevisionId);

    Optional<ApprovalDocumentJpaEntity> findByApprovalDocumentIdAndDeletedAtIsNull(Long approvalDocumentId);

    /** APR-006 중복 연결 확인 */
    boolean existsByApprovalRevisionIdAndFileVersionIdAndDeletedAtIsNull(
            Long approvalRevisionId, Long fileVersionId);

    /**
     * DEL-005 — 상위 블록 삭제 시 결재의 모든 문서 연결을 논리 삭제한다. APR-007의 DRAFT 수동
     * 연결 해제({@code deleteById})와 달리, 상신·완료 시점의 파일 버전 연결을 감사 이력으로 보존한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalDocumentJpaEntity d SET d.deletedAt = :deletedAt "
            + "WHERE d.approvalRevisionId IN "
            + "(SELECT r.approvalRevisionId FROM ApprovalRevisionJpaEntity r WHERE r.approvalId = :approvalId) "
            + "AND d.deletedAt IS NULL")
    void softDeleteAllByApprovalId(@Param("approvalId") Long approvalId,
                                   @Param("deletedAt") LocalDateTime deletedAt);
}

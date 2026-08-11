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
     * APR-007 — 기안자가 DRAFT 에서 문서 연결을 스스로 해제한다. 논리 삭제다(`DELETE.md` D-1·D-2 —
     * {@code approval_document}는 UNIQUE·복합 PK 가 없어 하드 대상인 「연결 행 7종」이 아니다).
     *
     * <p>같은 파일을 다시 붙일 수 있다 — 중복 검사가
     * {@link #existsByApprovalRevisionIdAndFileVersionIdAndDeletedAtIsNull} 로 활성 행만 보기 때문이다.
     *
     * <p>⚠️ 파일 영구삭제 잠금은 이 삭제를 상위 삭제와 <b>구분해야 한다</b>. 사용자가 뺀 문서는 잠금을
     * 풀어야 하고 상위 삭제로 보존된 문서는 잠금을 유지해야 한다. 구분 기준은 회차 생존이며
     * {@code mapper/file/ApprovalLockMapper.xml} 이 그 판별을 갖는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalDocumentJpaEntity d SET d.deletedAt = CURRENT_TIMESTAMP "
            + "WHERE d.approvalDocumentId = :documentId AND d.deletedAt IS NULL")
    void softDeleteById(@Param("documentId") Long documentId);

    /**
     * DEL-005 — 상위 블록 삭제 시 결재의 모든 문서 연결을 논리 삭제한다. 회차까지 함께 삭제되므로
     * 상신·완료 시점의 파일 버전 연결이 감사 이력으로 남고, 파일 영구삭제 잠금도 유지된다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalDocumentJpaEntity d SET d.deletedAt = :deletedAt "
            + "WHERE d.approvalRevisionId IN "
            + "(SELECT r.approvalRevisionId FROM ApprovalRevisionJpaEntity r WHERE r.approvalId = :approvalId) "
            + "AND d.deletedAt IS NULL")
    void softDeleteAllByApprovalId(@Param("approvalId") Long approvalId,
                                   @Param("deletedAt") LocalDateTime deletedAt);
}

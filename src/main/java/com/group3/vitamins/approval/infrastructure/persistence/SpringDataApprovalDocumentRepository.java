package com.group3.vitamins.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataApprovalDocumentRepository extends JpaRepository<ApprovalDocumentJpaEntity, Long> {

    List<ApprovalDocumentJpaEntity> findByApprovalRevisionId(Long approvalRevisionId);

    /** APR-006 중복 연결 확인 */
    boolean existsByApprovalRevisionIdAndFileVersionId(Long approvalRevisionId, Long fileVersionId);

    /**
     * 블록 삭제(`ApprovalBlockDetailAdapter.deleteDetail`) 시 호출 — 이 결재의 모든 회차에 연결된 문서를
     * 하드 삭제한다(APR-007과 같은 정책). {@code approval_document}는 {@code approval_id}를 직접 안 갖고
     * {@code approval_revision_id}로만 연결돼 있어 서브쿼리로 묶는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ApprovalDocumentJpaEntity d WHERE d.approvalRevisionId IN "
            + "(SELECT r.approvalRevisionId FROM ApprovalRevisionJpaEntity r WHERE r.approvalId = :approvalId)")
    void deleteAllByApprovalId(@Param("approvalId") Long approvalId);
}

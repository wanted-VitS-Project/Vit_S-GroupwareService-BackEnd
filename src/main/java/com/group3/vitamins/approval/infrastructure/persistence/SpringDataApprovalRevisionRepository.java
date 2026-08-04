package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataApprovalRevisionRepository extends JpaRepository<ApprovalRevisionJpaEntity, Long> {

    /** SUB-005~008 — 재상신 대상(REJECTED) 인지, 이미 준비된 DRAFT 회차가 있는지 판단하는 데 쓰는 최신 회차 */
    Optional<ApprovalRevisionJpaEntity> findTopByApprovalIdOrderByRevisionNoDesc(Long approvalId);

    /**
     * DRAFT 조건을 UPDATE 문 자체에 걸어 "확인 후 쓰기" 사이의 틈을 없앤다(`text.SpringDataTextRepository`와 동일 이유).
     * 그 틈에 상신(#7)돼 DRAFT 를 벗어났으면 0건 갱신되고, 그걸 409로 처리한다.
     * 벌크 UPDATE 는 {@code @UpdateTimestamp} 를 안 타므로 {@code updatedAt} 을 직접 SET 한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApprovalRevisionJpaEntity r SET r.title = :title, r.content = :content, "
            + "r.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE r.approvalRevisionId = :revisionId AND r.status = :draftStatus")
    int updateTitleContentIfDraft(@Param("revisionId") Long revisionId,
                                   @Param("title") String title,
                                   @Param("content") String content,
                                   @Param("draftStatus") ApprovalStatus draftStatus);
}

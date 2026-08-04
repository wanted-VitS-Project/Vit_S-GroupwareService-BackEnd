package com.group3.vitamins.checklist.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SpringDataChecklistRepository extends JpaRepository<ChecklistJpaEntity, Long> {

    /**
     * deleted_at 조건을 UPDATE 문 자체에 걸어서 "확인 후 쓰기" 사이의 틈을 없앤다.
     * 이미 삭제된 행이면 0을 반환한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistJpaEntity c SET c.content = :content, c.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE c.chkId = :chkId AND c.deletedAt IS NULL")
    int updateContentIfActive(@Param("chkId") Long chkId, @Param("content") String content);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistJpaEntity c SET c.completed = :completed, c.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE c.chkId = :chkId AND c.deletedAt IS NULL")
    int updateCompletionIfActive(@Param("chkId") Long chkId, @Param("completed") boolean completed);

    /**
     * 단건 삭제 — 이미 삭제된 행이면 0을 반환한다 (직접 삭제 API의 경합 판별용).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistJpaEntity c SET c.deletedAt = :deletedAt "
            + "WHERE c.chkId = :chkId AND c.deletedAt IS NULL")
    int markDeletedIfActive(@Param("chkId") Long chkId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 블록 삭제 이벤트로 인한 일괄 삭제 — 그 블록의 활성 항목 전부를 소프트 삭제한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistJpaEntity c SET c.deletedAt = :deletedAt "
            + "WHERE c.chkBlockId = :chkBlockId AND c.deletedAt IS NULL")
    int markAllDeletedByBlockIfActive(@Param("chkBlockId") Long chkBlockId, @Param("deletedAt") LocalDateTime deletedAt);

    long countByChkBlockIdAndDeletedAtIsNull(Long chkBlockId);

    long countByChkBlockIdAndCompletedTrueAndDeletedAtIsNull(Long chkBlockId);
}

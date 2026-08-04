package com.group3.vitamins.checklist.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SpringDataChecklistBlockRepository extends JpaRepository<ChecklistBlockJpaEntity, Long> {

    boolean existsByChkBlockIdAndDeletedAtIsNull(Long chkBlockId);

    /**
     * deleted_at 조건을 UPDATE 문 자체에 걸어서 "확인 후 쓰기" 사이의 틈을 없앤다.
     * 이미 삭제된 행이면 0을 반환한다 (중복 삭제 이벤트 판별용).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistBlockJpaEntity c SET c.deletedAt = :deletedAt "
            + "WHERE c.chkBlockId = :chkBlockId AND c.deletedAt IS NULL")
    int markDeletedIfActive(@Param("chkBlockId") Long chkBlockId, @Param("deletedAt") LocalDateTime deletedAt);
}

package com.group3.vitamins.checklist.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SpringDataChecklistBlockRepository extends JpaRepository<ChecklistBlockJpaEntity, Long> {

    /**
     * 항목 생성 직전에 쓰는 락 조회. PESSIMISTIC_WRITE 로 이 행을 잠가서, 같은 트랜잭션이
     * 끝날 때까지(=항목 INSERT 까지) 다른 트랜잭션이 이 블록을 동시에 삭제 처리하지 못하게 막는다.
     * 그렇지 않으면 "활성 확인 → 항목 생성" 사이에 블록이 삭제돼 죽은 블록 밑에 활성 항목이 생길 수 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChecklistBlockJpaEntity c WHERE c.chkBlockId = :chkBlockId AND c.deletedAt IS NULL")
    Optional<ChecklistBlockJpaEntity> findActiveForUpdate(@Param("chkBlockId") Long chkBlockId);

    /**
     * deleted_at 조건을 UPDATE 문 자체에 걸어서 "확인 후 쓰기" 사이의 틈을 없앤다.
     * 이미 삭제된 행이면 0을 반환한다 (중복 삭제 이벤트 판별용).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistBlockJpaEntity c SET c.deletedAt = :deletedAt "
            + "WHERE c.chkBlockId = :chkBlockId AND c.deletedAt IS NULL")
    int markDeletedIfActive(@Param("chkBlockId") Long chkBlockId, @Param("deletedAt") LocalDateTime deletedAt);

    @Query("SELECT c.blockId FROM ChecklistBlockJpaEntity c WHERE c.chkBlockId = :chkBlockId")
    Optional<Long> findBlockIdByChkBlockId(@Param("chkBlockId") Long chkBlockId);
}

package com.group3.vitamins.image.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SpringDataImageBlockRepository extends JpaRepository<ImageBlockJpaEntity, Long> {

    /**
     * 항목 생성 직전에 쓰는 락 조회. PESSIMISTIC_WRITE 로 이 행을 잠가서, 같은 트랜잭션이
     * 끝날 때까지(=항목 INSERT 까지) 다른 트랜잭션이 이 블록을 동시에 삭제 처리하지 못하게 막는다.
     * (체크리스트 도메인과 동일한 이유 — CatalogChecklistBlockAdapter 참고)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ImageBlockJpaEntity i WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    Optional<ImageBlockJpaEntity> findActiveForUpdate(@Param("imgBlockId") Long imgBlockId);

    /**
     * 락 없는 존재 확인 — 조회(GET)처럼 읽기 전용 트랜잭션에서 쓴다. {@link #findActiveForUpdate} 는
     * PESSIMISTIC_WRITE 라 읽기 전용 트랜잭션에서 호출하면 DB가 거부한다(MySQL "READ ONLY transaction").
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ImageBlockJpaEntity i "
            + "WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    boolean existsActiveNoLock(@Param("imgBlockId") Long imgBlockId);

    /**
     * deleted_at 조건을 UPDATE 문 자체에 걸어서 "확인 후 쓰기" 사이의 틈을 없앤다.
     * 이미 삭제된 행이면 0을 반환한다 (중복 삭제 이벤트 판별용).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImageBlockJpaEntity i SET i.deletedAt = :deletedAt "
            + "WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    int markDeletedIfActive(@Param("imgBlockId") Long imgBlockId, @Param("deletedAt") LocalDateTime deletedAt);
}

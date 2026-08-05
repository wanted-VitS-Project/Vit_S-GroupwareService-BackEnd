package com.group3.vitamins.image.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataImageRepository extends JpaRepository<ImageJpaEntity, Long> {

    @Query("SELECT COALESCE(MAX(i.orderIndex), 0) FROM ImageJpaEntity i "
            + "WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    int findMaxOrderIndex(@Param("imgBlockId") Long imgBlockId);

    @Query("SELECT i FROM ImageJpaEntity i WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    List<ImageJpaEntity> findAllActiveByImgBlockId(@Param("imgBlockId") Long imgBlockId);

    // @UpdateTimestamp 는 벌크 JPQL UPDATE 에는 적용되지 않는다(Hibernate 특성 — 엔티티 생명주기를
    // 안 타고 SQL로 바로 나감) — 체크리스트 도메인과 동일한 이유로 updatedAt 을 쿼리에서 직접 찍는다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImageJpaEntity i SET i.caption = :caption, i.orderIndex = :orderIndex, i.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE i.imgId = :imgId AND i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    int updateCaptionAndOrder(@Param("imgId") Long imgId, @Param("imgBlockId") Long imgBlockId,
                              @Param("caption") String caption, @Param("orderIndex") int orderIndex);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImageJpaEntity i SET i.deletedAt = :deletedAt "
            + "WHERE i.imgId = :imgId AND i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    int markDeleted(@Param("imgId") Long imgId, @Param("imgBlockId") Long imgBlockId,
                     @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 블록 삭제 이벤트로 인한 일괄 삭제 — 그 블록의 활성 항목 전부를 소프트 삭제한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImageJpaEntity i SET i.deletedAt = :deletedAt "
            + "WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    int markAllDeletedByBlockIfActive(@Param("imgBlockId") Long imgBlockId, @Param("deletedAt") LocalDateTime deletedAt);
}

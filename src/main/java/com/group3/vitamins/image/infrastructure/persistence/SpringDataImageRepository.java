package com.group3.vitamins.image.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
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

    /** Pageable로 "1건만" 받는다 — JPQL은 표준적으로 LIMIT을 지원하지 않아 이 방식이 더 이식성이 좋다. */
    @Query("SELECT i FROM ImageJpaEntity i WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL "
            + "AND i.orderIndex > :orderIndex ORDER BY i.orderIndex ASC")
    List<ImageJpaEntity> findActiveAfterOrderIndex(@Param("imgBlockId") Long imgBlockId,
                                                    @Param("orderIndex") int orderIndex, Pageable pageable);

    @Query("SELECT i FROM ImageJpaEntity i WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL "
            + "AND i.orderIndex < :orderIndex ORDER BY i.orderIndex DESC")
    List<ImageJpaEntity> findActiveBeforeOrderIndex(@Param("imgBlockId") Long imgBlockId,
                                                     @Param("orderIndex") int orderIndex, Pageable pageable);

    @Query("SELECT i FROM ImageJpaEntity i WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL "
            + "ORDER BY i.orderIndex ASC")
    List<ImageJpaEntity> findActiveOrderByOrderIndexAsc(@Param("imgBlockId") Long imgBlockId, Pageable pageable);

    @Query("SELECT i FROM ImageJpaEntity i WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL "
            + "ORDER BY i.orderIndex DESC")
    List<ImageJpaEntity> findActiveOrderByOrderIndexDesc(@Param("imgBlockId") Long imgBlockId, Pageable pageable);

    @Query("SELECT COUNT(i) FROM ImageJpaEntity i WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    int countActiveByImgBlockId(@Param("imgBlockId") Long imgBlockId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ImageJpaEntity i "
            + "WHERE i.imgBlockId = :imgBlockId AND i.orderIndex = :orderIndex AND i.deletedAt IS NULL")
    boolean existsActiveByOrderIndex(@Param("imgBlockId") Long imgBlockId, @Param("orderIndex") int orderIndex);

    /** 삭제 필터 없음 — 복구 API가 "존재 자체가 없음"과 "이미 활성 상태"를 구분하는 데 쓴다. */
    @Query("SELECT i FROM ImageJpaEntity i WHERE i.imgId IN :imgIds")
    List<ImageJpaEntity> findAllByImgIdIn(@Param("imgIds") List<Long> imgIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImageJpaEntity i SET i.deletedAt = NULL, i.orderIndex = :orderIndex, i.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE i.imgId = :imgId AND i.imgBlockId = :imgBlockId AND i.deletedAt IS NOT NULL")
    int restore(@Param("imgId") Long imgId, @Param("imgBlockId") Long imgBlockId, @Param("orderIndex") int orderIndex);

    /** 소프트 삭제된(휴지통) 항목만 대상으로 하는 조건부 하드 삭제. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ImageJpaEntity i WHERE i.imgId = :imgId AND i.deletedAt IS NOT NULL")
    int hardDelete(@Param("imgId") Long imgId);
}

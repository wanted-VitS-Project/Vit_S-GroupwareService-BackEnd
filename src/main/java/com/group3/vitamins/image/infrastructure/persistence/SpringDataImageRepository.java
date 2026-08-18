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

    // ORDER BY 는 전체 조회 API가 orderIndex 오름차순을 계약으로 내려주기 위해 필요하다(2026-08-07 추가).
    // 기존 사용처(수정 API의 대조용 Map, zip 다운로드)는 순서에 의존하지 않아 영향 없다 —
    // 오히려 zip 안 파일 순서가 정렬 순서로 고정되는 부수 효과가 있다.
    @Query("SELECT i FROM ImageJpaEntity i WHERE i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL "
            + "ORDER BY i.orderIndex ASC")
    List<ImageJpaEntity> findAllActiveByImgBlockId(@Param("imgBlockId") Long imgBlockId);

    // @UpdateTimestamp 는 벌크 JPQL UPDATE 에는 적용되지 않는다(Hibernate 특성 — 엔티티 생명주기를
    // 안 타고 SQL로 바로 나감) — 체크리스트 도메인과 동일한 이유로 updatedAt 을 쿼리에서 직접 찍는다.
    // clearAutomatically·flushAutomatically 를 빼면 같은 트랜잭션의 영속성 컨텍스트가 옛 값을 들고
    // 있어 UPDATE 후 재조회에서도 낡은 값이 나온다(CONCURRENCY.md §6-2).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ImageJpaEntity i SET i.caption = :caption, i.orderIndex = :orderIndex, "
            + "i.updatedAt = CURRENT_TIMESTAMP, i.version = i.version + 1 "
            + "WHERE i.imgId = :imgId AND i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL "
            + "AND i.version = :expectedVersion")
    int updateCaptionAndOrderIfVersionMatches(@Param("imgId") Long imgId, @Param("imgBlockId") Long imgBlockId,
                              @Param("caption") String caption, @Param("orderIndex") int orderIndex,
                              @Param("expectedVersion") int expectedVersion);

    // 값은 안 바뀌었지만 version은 검사해야 하는 항목용 — caption/orderIndex/updatedAt은 안 건드리고
    // version만 검사 후 증가시킨다(2026-08-11, CodeRabbit 지적).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ImageJpaEntity i SET i.version = i.version + 1 "
            + "WHERE i.imgId = :imgId AND i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL "
            + "AND i.version = :expectedVersion")
    int touchVersionIfMatches(@Param("imgId") Long imgId, @Param("imgBlockId") Long imgBlockId,
                              @Param("expectedVersion") int expectedVersion);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImageJpaEntity i SET i.deletedAt = :deletedAt "
            + "WHERE i.imgId = :imgId AND i.imgBlockId = :imgBlockId AND i.deletedAt IS NULL")
    int markDeleted(@Param("imgId") Long imgId, @Param("imgBlockId") Long imgBlockId,
                     @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 단건 삭제 후 뒤쪽 이미지들의 orderIndex를 1씩 당겨 빈자리를 없앤다(2026-08-17, 프론트 요청).
     * updatedAt은 건드리지 않는다 — 사용자가 직접 손댄 필드가 아니라 삭제의 부수 효과로 밀린 것뿐이다.
     *
     * ⚠️ version은 올린다(2026-08-18, CodeRabbit 지적) — 이 압축 직전에 이 블록의 이미지 목록을
     * 조회해 간 PATCH(수정 API)가 압축 전 orderIndex를 들고 동시에 저장을 시도하면, version을 안
     * 올릴 경우 그 stale 값이 버전 체크를 통과해 방금 압축한 순서를 조용히 덮어쓸 수 있다. version을
     * 올려두면 그 PATCH는 버전 충돌(409)로 깔끔하게 걸러진다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImageJpaEntity i SET i.orderIndex = i.orderIndex - 1, i.version = i.version + 1 "
            + "WHERE i.imgBlockId = :imgBlockId AND i.orderIndex > :orderIndex AND i.deletedAt IS NULL")
    int decrementOrderIndexAfter(@Param("imgBlockId") Long imgBlockId, @Param("orderIndex") int orderIndex);

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

    /**
     * 소프트 삭제된(휴지통) 항목만 대상으로 하는 조건부 하드 삭제 — 여러 건을 한 번에 지운다
     * (2026-08-16 — 건마다 DELETE를 반복 호출하던 걸 IN절 배치 하나로 통합).
     *
     * @return 실제로 삭제된 행 수 — 요청한 imgIds 개수보다 적으면 그 사이 레이스(이미 복구됨 등)가 있었다는 뜻
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ImageJpaEntity i WHERE i.imgId IN :imgIds AND i.deletedAt IS NOT NULL")
    int hardDeleteAll(@Param("imgIds") List<Long> imgIds);
}

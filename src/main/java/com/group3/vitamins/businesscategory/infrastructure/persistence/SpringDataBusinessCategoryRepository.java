package com.group3.vitamins.businesscategory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataBusinessCategoryRepository
        extends JpaRepository<BusinessCategoryJpaEntity, Long> {

    /** 이름 오름차순 목록. keyword 가 null 이면 전건, includeDeleted 가 false 면 삭제분을 뺀다. */
    @Query("""
            SELECT c FROM BusinessCategoryJpaEntity c
            WHERE (:includeDeleted = TRUE OR c.deletedAt IS NULL)
              AND (:keyword IS NULL
                   OR c.name LIKE CONCAT('%', :keyword, '%')
                   OR c.code LIKE CONCAT('%', :keyword, '%'))
            ORDER BY c.name ASC
            """)
    List<BusinessCategoryJpaEntity> search(@Param("keyword") String keyword,
                                           @Param("includeDeleted") boolean includeDeleted);

    /** 삭제분 포함 전체에서 이름으로 찾는다 (deleted_at 조건 없음 — §3-1). */
    Optional<BusinessCategoryJpaEntity> findByName(String name);

    /** 삭제분 포함 전체에서 업무코드로 찾는다 (deleted_at 조건 없음 — §3-1). */
    Optional<BusinessCategoryJpaEntity> findByCode(String code);

    /** 삭제되지 않은 것만 ID로 찾는다 (PATCH·DELETE 의 404 판정용). */
    Optional<BusinessCategoryJpaEntity> findByBusinessCategoryIdAndDeletedAtIsNull(Long businessCategoryId);
}
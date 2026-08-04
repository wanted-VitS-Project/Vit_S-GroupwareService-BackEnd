package com.group3.vitamins.businesscategory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
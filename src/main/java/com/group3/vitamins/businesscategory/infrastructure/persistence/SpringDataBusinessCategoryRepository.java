package com.group3.vitamins.businesscategory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataBusinessCategoryRepository
        extends JpaRepository<BusinessCategoryJpaEntity, Long> {

    /** 회사 범위 이름 오름차순 목록. keyword 가 null 이면 전건, includeDeleted 가 false 면 삭제분을 뺀다. */
    @Query("""
            SELECT c FROM BusinessCategoryJpaEntity c
            WHERE c.companyId = :companyId
              AND (:includeDeleted = TRUE OR c.deletedAt IS NULL)
              AND (:keyword IS NULL
                   OR c.name LIKE CONCAT('%', :keyword, '%')
                   OR c.code LIKE CONCAT('%', :keyword, '%'))
            ORDER BY c.name ASC
            """)
    List<BusinessCategoryJpaEntity> search(@Param("keyword") String keyword,
                                           @Param("includeDeleted") boolean includeDeleted,
                                           @Param("companyId") Long companyId);

    /**
     * 회사 범위에서 활성(deleted_at IS NULL) 카테고리를 이름으로 찾는다 — 중복 검사용.
     * UNIQUE 를 걷은 뒤(DELETE.md §6-1) 삭제분은 같은 이름이 여러 개 쌓일 수 있어,
     * 삭제분까지 조회하면 Optional 이 NonUniqueResult 로 터진다. 활성만 보면 최대 1건이라 안전하다.
     */
    Optional<BusinessCategoryJpaEntity> findByNameAndCompanyIdAndDeletedAtIsNull(String name, Long companyId);

    /** 회사 범위에서 활성(deleted_at IS NULL) 카테고리를 업무코드로 찾는다 — 중복 검사용. */
    Optional<BusinessCategoryJpaEntity> findByCodeAndCompanyIdAndDeletedAtIsNull(String code, Long companyId);

    /** 회사 범위에서 삭제되지 않은 것만 ID로 찾는다 (PATCH·DELETE 의 404 판정용). */
    Optional<BusinessCategoryJpaEntity> findByBusinessCategoryIdAndCompanyIdAndDeletedAtIsNull(
            Long businessCategoryId, Long companyId);
}

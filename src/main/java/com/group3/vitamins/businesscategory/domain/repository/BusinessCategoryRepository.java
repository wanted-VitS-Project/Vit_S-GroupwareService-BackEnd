package com.group3.vitamins.businesscategory.domain.repository;

import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;

import java.util.List;
import java.util.Optional;

public interface BusinessCategoryRepository {

    /**
     * 이름 오름차순 목록. 페이징 없이 전건을 내린다.
     *
     * @param keyword        이름·업무코드 부분 일치. null 이면 전건
     * @param includeDeleted true 면 논리 삭제분까지 포함
     * @param companyId      회사(테넌트) 범위
     */
    List<BusinessCategory> search(String keyword, boolean includeDeleted, Long companyId);

    /** 회사 범위에서 활성(deleted_at IS NULL) 카테고리를 이름으로 조회한다 (중복 검사용 · DELETE.md §6-1). */
    Optional<BusinessCategory> findActiveByName(String name, Long companyId);

    /** 회사 범위에서 활성(deleted_at IS NULL) 카테고리를 업무코드로 조회한다 (중복 검사용 · DELETE.md §6-1). */
    Optional<BusinessCategory> findActiveByCode(String code, Long companyId);

    /** 새로 만들거나 변경된 카테고리를 저장한다. */
    BusinessCategory save(BusinessCategory category);

    /**
     * 삭제되지 않은 카테고리를 회사 범위로 조회한다.
     * 삭제된 건과 타사 카테고리는 404 로 취급한다.
     */
    Optional<BusinessCategory> findActiveById(Long categoryId, Long companyId);
}

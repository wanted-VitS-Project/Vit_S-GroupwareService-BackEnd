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
     */
    List<BusinessCategory> search(String keyword, boolean includeDeleted);

    /** 이름으로 조회한다. 삭제분도 포함해서 찾는다 (§3-1 중복 검사용). */
    Optional<BusinessCategory> findByName(String name);

    /** 업무코드로 조회한다. 삭제분도 포함해서 찾는다 (§3-1 중복 검사용). */
    Optional<BusinessCategory> findByCode(String code);

    /** 새로 만들거나 변경된 카테고리를 저장한다. */
    BusinessCategory save(BusinessCategory category);

    /** 삭제되지 않은 카테고리를 ID로 조회한다. 삭제된 건은 404 로 취급한다. */
    Optional<BusinessCategory> findActiveById(Long categoryId);
}
package com.group3.vitamins.businesscategory.domain.repository;

import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;

import java.util.List;

public interface BusinessCategoryRepository {

    /**
     * 이름 오름차순 목록. 페이징 없이 전건을 내린다.
     *
     * @param keyword        이름·업무코드 부분 일치. null 이면 전건
     * @param includeDeleted true 면 논리 삭제분까지 포함
     */
    List<BusinessCategory> search(String keyword, boolean includeDeleted);
}
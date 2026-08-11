
package com.group3.vitamins.businesscategory.application.usecase;

import com.group3.vitamins.businesscategory.application.query.BusinessCategoryListQuery;
import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;

import java.util.List;

public interface BusinessCategoryQueryUseCase {

    /**
     * 사업 카테고리 목록을 이름 오름차순으로 조회한다.
     * 삭제분 포함 조회는 ADMIN 만 가능하다.
     */
    List<BusinessCategoryResult> listCategories(BusinessCategoryListQuery query);
}
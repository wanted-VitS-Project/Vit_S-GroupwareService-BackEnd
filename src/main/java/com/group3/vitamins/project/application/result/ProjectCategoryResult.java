package com.group3.vitamins.project.application.result;

import java.util.List;

/** 카테고리 연결 결과. 연결 후 <b>전체</b> 카테고리를 담는다 — 방금 추가한 것만이 아니다. */
public record ProjectCategoryResult(
        Long projectId,
        List<BusinessCategorySummary> businessCategories
) {
}

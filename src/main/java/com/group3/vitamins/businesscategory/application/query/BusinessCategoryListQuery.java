package com.group3.vitamins.businesscategory.application.query;

public record BusinessCategoryListQuery(
        String keyword,
        boolean includeDeleted,
        String role
) {

    /** 공백·빈 문자열 keyword 를 null 로 눕혀 "검색 안 함" 분기를 하나로 모은다. */
    public BusinessCategoryListQuery {
        keyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }
}
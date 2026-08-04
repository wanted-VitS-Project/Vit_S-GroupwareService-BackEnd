package com.group3.vitamins.businesscategory.application.result;

import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;

import java.time.LocalDateTime;

public record BusinessCategoryResult(
        Long categoryId,
        String name,
        String code,
        String description,
        boolean deletable,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {

    /** 도메인 객체와 삭제 가능 여부로 결과를 만든다. */
    public static BusinessCategoryResult of(BusinessCategory category, boolean deletable) {
        return new BusinessCategoryResult(
                category.getBusinessCategoryId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                deletable,
                category.getCreatedAt(),
                category.getDeletedAt()
        );
    }
}
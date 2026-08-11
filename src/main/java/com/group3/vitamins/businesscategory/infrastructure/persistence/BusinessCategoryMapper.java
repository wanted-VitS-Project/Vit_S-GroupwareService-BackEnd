package com.group3.vitamins.businesscategory.infrastructure.persistence;

import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;

public class BusinessCategoryMapper {

    private BusinessCategoryMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static BusinessCategory toDomain(BusinessCategoryJpaEntity entity) {
        return BusinessCategory.restore(
                entity.getBusinessCategoryId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getDeletedAt()
        );
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static BusinessCategoryJpaEntity toEntity(BusinessCategory domain) {
        return new BusinessCategoryJpaEntity(
                domain.getBusinessCategoryId(),
                domain.getCompanyId(),
                domain.getName(),
                domain.getCode(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getDeletedAt()
        );
    }
}
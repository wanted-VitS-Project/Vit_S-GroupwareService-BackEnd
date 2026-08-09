package com.group3.vitamins.employeegroup.infrastructure.persistence;

import com.group3.vitamins.employeegroup.domain.model.EmployeeGroup;

/** JPA 엔티티 ↔ 도메인 객체 변환기 (정적 유틸). */
public final class EmployeeGroupPersistenceMapper {

    private EmployeeGroupPersistenceMapper() {
    }

    public static EmployeeGroup toDomain(EmployeeGroupJpaEntity entity) {
        return EmployeeGroup.restore(
                entity.getGroupId(), entity.getName(), entity.getDescription(), entity.getCreatedBy());
    }

    public static EmployeeGroupJpaEntity toEntity(EmployeeGroup domain) {
        return new EmployeeGroupJpaEntity(
                domain.getGroupId(), domain.getName(), domain.getDescription(), domain.getCreatedBy());
    }
}

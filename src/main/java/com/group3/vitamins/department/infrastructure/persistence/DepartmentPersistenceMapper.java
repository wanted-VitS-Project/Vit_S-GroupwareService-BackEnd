package com.group3.vitamins.department.infrastructure.persistence;

import com.group3.vitamins.department.domain.model.Department;

/**
 * JPA 엔티티 ↔ 도메인 객체 변환기. 정적 유틸이라 상태가 없다.
 * (부서·사원 조인 조회의 {@code employee} 매핑은 MyBatis 소관이라 여기 없다.)
 */
public final class DepartmentPersistenceMapper {

    private DepartmentPersistenceMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static Department toDomain(DepartmentJpaEntity entity) {
        return Department.restore(
                entity.getDepartmentId(),
                entity.getName(),
                entity.getParentId());
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static DepartmentJpaEntity toEntity(Department domain) {
        return new DepartmentJpaEntity(
                domain.getDepartmentId(),
                domain.getName(),
                domain.getParentId());
    }
}

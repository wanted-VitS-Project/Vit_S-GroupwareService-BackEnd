package com.group3.vitamins.employee.infrastructure.persistence;

import com.group3.vitamins.employee.domain.model.Employee;

/**
 * JPA 엔티티 ↔ 도메인 객체 변환기. 정적 유틸이라 상태가 없다.
 * (계정·부서·직급 조인 조회의 매핑은 MyBatis 소관이라 여기 없다.)
 */
public final class EmployeePersistenceMapper {

    private EmployeePersistenceMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static Employee toDomain(EmployeeJpaEntity entity) {
        return Employee.restore(
                entity.getUserId(),
                entity.getName(),
                entity.isSystem(),
                entity.getDepartmentId(),
                entity.getJobPositionId(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getHiredAt(),
                entity.getResignedAt(),
                entity.getCompanyId());
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static EmployeeJpaEntity toEntity(Employee domain) {
        return new EmployeeJpaEntity(
                domain.getUserId(),
                domain.getName(),
                domain.isSystem(),
                domain.getDepartmentId(),
                domain.getJobPositionId(),
                domain.getEmail(),
                domain.getPhone(),
                domain.getHiredAt(),
                domain.getResignedAt(),
                domain.getCompanyId());
    }
}

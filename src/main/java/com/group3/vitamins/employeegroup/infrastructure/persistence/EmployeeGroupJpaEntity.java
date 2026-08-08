package com.group3.vitamins.employeegroup.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그룹 JPA 엔티티. 팀 ERD 의 {@code employee_group} 테이블 (이미 존재 — 새 마이그레이션 없음).
 * 도메인 로직은 {@link com.group3.vitamins.employeegroup.domain.model.EmployeeGroup} 가 갖는다.
 * {@code created_at}·{@code updated_at} 은 DB 가 채우고 목록은 MyBatis 로 읽으므로 매핑하지 않는다.
 */
@Entity
@Table(name = "employee_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_employee_group_name", columnNames = "name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeGroupJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_by", nullable = false, length = 20)
    private String createdBy;

    public EmployeeGroupJpaEntity(Long groupId, String name, String description, String createdBy) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }
}

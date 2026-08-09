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
import org.hibernate.annotations.DynamicUpdate;

/**
 * 그룹 JPA 엔티티. 팀 ERD 의 {@code employee_group} 테이블 (이미 존재 — 새 마이그레이션 없음).
 * 도메인 로직은 {@link com.group3.vitamins.employeegroup.domain.model.EmployeeGroup} 가 갖는다.
 * {@code created_at}·{@code updated_at} 은 DB 가 채우고 목록은 MyBatis 로 읽으므로 매핑하지 않는다.
 *
 * <p>{@code @DynamicUpdate} — 수정(§3)은 이름·설명 중 전달한 필드만 바꾼다. 동적 UPDATE 로 <b>바뀐 컬럼만</b> 갱신해,
 * 두 요청이 이름·설명을 각각 수정할 때 서로의 변경을 덮어쓰는 lost-update 를 막는다(AccountEntity 선례 · @Version 없이 스키마 무변경).
 */
@Entity
@DynamicUpdate
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

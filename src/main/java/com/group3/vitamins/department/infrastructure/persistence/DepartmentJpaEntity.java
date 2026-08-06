package com.group3.vitamins.department.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 JPA 엔티티. 팀 ERD 의 {@code department} 테이블 (이미 존재 — 새 마이그레이션 없음).
 *
 * <p>도메인 로직은 {@link com.group3.vitamins.department.domain.model.Department} 가 갖고, 이 클래스는
 * 순수 영속 매핑만 한다. self FK 를 {@code @ManyToOne} 대신 {@code parentId} 원시 컬럼으로 둔다 —
 * 명세가 부서를 {@code parentId} 로만 다루고 지연로딩·프록시가 필요 없다.
 *
 * <p>{@code created_at}·{@code updated_at} 은 DB 가 채우고 응답에 나가지 않으므로 매핑하지 않는다
 * (매핑하지 않은 DB 컬럼은 {@code ddl-auto: validate} 에 걸리지 않는다).
 */
@Entity
@Table(name = "department",
        // (parent_id, name) 복합 유니크 = DB 의 uk_department_parent_name 을 엔티티에도 명시한다(2026-08-06).
        // validate 는 UNIQUE 를 검사하지 않아 운영엔 영향이 없고, 테스트(create-drop)에서 제약이 생겨
        // "같은 부모 아래 동명"(=자식 부서 중복)이 실제로 막힌다. ⚠️ MySQL/H2 모두 parent_id 가 NULL 인
        // 행끼리는 UNIQUE 로 안 막으므로 최상위 동명은 서비스가 app 레벨로 막는다(DepartmentCommandService).
        uniqueConstraints = @UniqueConstraint(
                name = "uk_department_parent_name", columnNames = {"parent_id", "name"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 상위 부서. {@code null} 이면 최상위 */
    @Column(name = "parent_id")
    private Long parentId;
}

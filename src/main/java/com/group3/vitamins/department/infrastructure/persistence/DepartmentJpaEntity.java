package com.group3.vitamins.department.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "department")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    // unique=true 로 DB 의 uk_department_name 을 엔티티에도 명시한다. validate 는 UNIQUE 를 검사하지
    // 않으므로 운영엔 영향이 없고, 테스트(create-drop)에서 제약이 생겨 중복 저장(수정 경로 포함)이 실제로 막힌다.
    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    /** 상위 부서. {@code null} 이면 최상위 */
    @Column(name = "parent_id")
    private Long parentId;
}

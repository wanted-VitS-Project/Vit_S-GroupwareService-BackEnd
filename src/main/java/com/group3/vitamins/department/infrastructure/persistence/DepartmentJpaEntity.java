package com.group3.vitamins.department.infrastructure.persistence;

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
import org.hibernate.annotations.GeneratedColumn;

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
        // (company_id, parent_key, name) 복합 유니크 = DB 의 uk_department_company_parent_name 을 엔티티에도
        // 명시한다(마이그레이션 V20260809101000 에서 회사 범위로 확장). validate 는 UNIQUE 를 검사하지 않아
        // 운영엔 영향이 없고, 테스트(create-drop)에서 제약이 생겨 "같은 회사·같은 상위 부서 안" 동명만 막힌다
        // — 회사가 다르면 같은 최상위 부서명도 허용돼야 하므로 company_id 를 키에 포함해야 한다.
        // parent_key(=COALESCE(parent_id,0)) 를 쓰는 이유 — MySQL/H2 는 parent_id 가 NULL 인 행끼리 UNIQUE 로
        // 안 막아 최상위 동명이 새므로, NULL 을 0 으로 정규화해 최상위(0 공유)까지 DB 가 막게 한다.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_department_company_parent_name", columnNames = {"company_id", "parent_key", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 상위 부서. {@code null} 이면 최상위 */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 유니크용 상위키. DB 생성 열 {@code COALESCE(parent_id, 0)} — 최상위(부모 없음)는 0 을 공유해
     * 최상위 동명까지 복합 유니크가 막는다. 앱이 쓰지 않는 읽기 전용 파생 값이라 도메인 객체엔 없다.
     * {@code @GeneratedColumn} 이 create-drop 스키마에도 이 생성 열을 만들어 테스트에서 제약이 성립한다.
     */
    @GeneratedColumn("COALESCE(parent_id, 0)")
    @Column(name = "parent_key", insertable = false, updatable = false)
    private Long parentKey;

    public DepartmentJpaEntity(Long departmentId, Long companyId, String name, Long parentId) {
        this.departmentId = departmentId;
        this.companyId = companyId;
        this.name = name;
        this.parentId = parentId;
    }
}

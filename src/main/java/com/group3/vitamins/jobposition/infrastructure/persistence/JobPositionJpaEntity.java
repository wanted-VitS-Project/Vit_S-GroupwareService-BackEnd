package com.group3.vitamins.jobposition.infrastructure.persistence;

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
 * {@code job_position} 테이블 매핑.
 *
 * <p>{@code created_at}·{@code updated_at} 은 DB 기본값({@code DEFAULT CURRENT_TIMESTAMP} ·
 * {@code ON UPDATE})이 관리하므로 매핑하지 않는다. {@code ddl-auto: validate} 는 매핑된 컬럼만 검증하니
 * 매핑하지 않은 컬럼이 있어도 무방하다 — 응답에 쓰이지 않는 감사 컬럼을 JPA 로 끌고 다니지 않는다.
 */
@Entity
// (company_id, name) 복합 유니크 = DB 의 uk_job_position_company_name 을 엔티티에도 명시한다. validate 는
// UNIQUE 를 검사하지 않아 운영엔 영향이 없고, 테스트(create-drop)에서 제약이 생겨 회사 스코프 중복이 막힌다.
@Table(name = "job_position",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_position_company_name", columnNames = {"company_id", "name"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPositionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_position_id")
    private Long jobPositionId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}

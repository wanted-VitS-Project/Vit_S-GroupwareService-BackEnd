package com.group3.vitamins.department.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서. 팀 ERD 의 {@code department} 테이블 (이미 존재 — 새 마이그레이션 없음).
 *
 * <p>계층은 <b>최대 2단</b>이다. {@code parentId} 가 {@code null} 이면 최상위 부서다.
 * self FK 를 {@code @ManyToOne} 대신 {@code parentId} 원시 컬럼으로 두는 이유 —
 * 명세가 부서를 {@code parentId} 로만 다루고(트리 조립은 조회에서), 지연로딩·프록시가 필요 없다.
 *
 * <p>{@code created_at}·{@code updated_at} 은 DB 가 채우고 응답에 나가지 않으므로 매핑하지 않는다
 * (매핑하지 않은 DB 컬럼은 {@code ddl-auto: validate} 에 걸리지 않는다).
 */
@Entity
@Table(name = "department")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 상위 부서. {@code null} 이면 최상위 */
    @Column(name = "parent_id")
    private Long parentId;

    private DepartmentEntity(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    /** 부서 생성. {@code parentId} 가 {@code null} 이면 최상위, 있으면 하위 부서 */
    public static DepartmentEntity create(String name, Long parentId) {
        return new DepartmentEntity(name, parentId);
    }

    /** 부서명 수정 — 상위 부서는 바꾸지 않는다 (부서 이동 기능 없음, `.ai/api/department.md` §3) */
    public void rename(String name) {
        this.name = name;
    }

    public boolean isRoot() {
        return parentId == null;
    }
}

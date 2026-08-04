package com.group3.vitamins.department.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 부서 쓰기·단건 조회 (JPA). 트리·인원 집계처럼 {@code employee} 를 가로지르는 조회는
 * {@link DepartmentQueryMapper}(MyBatis)가 맡는다. 역할을 섞지 마라.
 */
public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, Long> {

    /** 부서명 전체 유니크 검증 (생성) */
    boolean existsByName(String name);

    /** 부서명 유니크 검증 (수정) — 자기 자신은 제외한다 */
    boolean existsByNameAndDepartmentIdNot(String name, Long departmentId);

    /** 직속 하위 부서 수 — 삭제 차단 판정 */
    long countByParentId(Long parentId);
}

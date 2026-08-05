package com.group3.vitamins.department.domain.repository;

import com.group3.vitamins.department.domain.model.Department;

import java.util.Optional;

/**
 * 부서 쓰기·단건 조회 아웃바운드 포트. 도메인은 이 인터페이스만 알고, 실제 영속은
 * {@code infrastructure/persistence} 어댑터가 처리한다.
 *
 * <p>트리·인원 집계처럼 {@code employee} 를 가로지르는 조회는 여기가 아니라
 * {@link com.group3.vitamins.department.application.port.DepartmentEmployeeQueryPort}(MyBatis)가 맡는다.
 * 역할을 섞지 마라.
 */
public interface DepartmentRepository {

    /**
     * 부서를 저장한다. 구현은 {@code saveAndFlush} 로 즉시 반영해, 유니크 제약 위반
     * ({@code uk_department_name})을 커밋까지 미루지 않고 이 시점에 드러낸다 — 서비스가 그 위반을
     * 명세의 409({@code DEPT_NAME_DUPLICATED})로 변환할 수 있게 한다.
     */
    Department save(Department department);

    /** 단건 조회 (수정·상위 부서명 확인용). */
    Optional<Department> findById(Long departmentId);

    /**
     * <b>비관적 쓰기 잠금</b> 단건 조회. 부서 행을 잠가, 생성 시 부모 삭제 레이스나 삭제 시 사원 배정·
     * 하위 부서 생성 레이스로 FK 위반(500)이 나는 것을 직렬화로 막는다 (account 의 {@code findByUserIdForUpdate} 선례).
     */
    Optional<Department> findByIdForUpdate(Long departmentId);

    /** 부서명 전체 유니크 검증 (생성). */
    boolean existsByName(String name);

    /** 부서명 유니크 검증 (수정) — 자기 자신은 제외한다. */
    boolean existsByNameAndDepartmentIdNot(String name, Long departmentId);

    /** 직속 하위 부서 수 — 삭제 차단 판정. */
    long countByParentId(Long parentId);

    /** 부서를 제거한다 (소프트 삭제 아님). */
    void delete(Department department);
}

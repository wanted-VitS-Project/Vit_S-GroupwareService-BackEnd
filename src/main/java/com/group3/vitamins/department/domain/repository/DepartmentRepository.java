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
     * ({@code uk_department_parent_name(parent_key, name)} = 같은 상위 부서 안 동명)을 커밋까지 미루지
     * 않고 이 시점에 드러낸다 — 서비스가 그 위반을 명세의 409({@code DEPT_NAME_DUPLICATED})로 변환할 수
     * 있게 한다. {@code parent_key=COALESCE(parent_id,0)} 라 최상위·자식 동명 모두 이 DB 제약이 잡는다.
     */
    Department save(Department department);

    /** 회사 범위 단건 조회 (수정·상위 부서명 확인용). 타사 부서는 조회되지 않아 404 로 귀결된다. */
    Optional<Department> findById(Long departmentId, Long companyId);

    /**
     * <b>비관적 쓰기 잠금</b> 회사 범위 단건 조회. 부서 행을 잠가, 생성 시 부모 삭제 레이스나 삭제 시 사원 배정·
     * 하위 부서 생성 레이스로 FK 위반(500)이 나는 것을 직렬화로 막는다 (account 의 {@code findByUserIdForUpdate} 선례).
     * 타사 부서는 조회되지 않아 부모/삭제 대상 404 로 귀결된다.
     */
    Optional<Department> findByIdForUpdate(Long departmentId, Long companyId);

    /**
     * 같은 회사·같은 상위 부서 안에서 부서명 중복 검증 (생성). {@code parentId} 가 {@code null} 이면
     * 회사 안 최상위 형제(부모 없는 부서)끼리 비교한다.
     */
    boolean existsSiblingName(String name, Long parentId, Long companyId);

    /**
     * 같은 회사·같은 상위 부서 안에서 부서명 중복 검증 (수정) — 자기 자신은 제외한다.
     * 상위는 바뀌지 않으므로 그 부서의 현재 {@code parentId} 기준 형제끼리 비교한다.
     */
    boolean existsSiblingNameExcludingSelf(String name, Long parentId, Long departmentId, Long companyId);

    /** 회사 범위 직속 하위 부서 수 — 삭제 차단 판정. */
    long countByParentId(Long parentId, Long companyId);

    /** 부서를 제거한다 (소프트 삭제 아님). */
    void delete(Department department);
}

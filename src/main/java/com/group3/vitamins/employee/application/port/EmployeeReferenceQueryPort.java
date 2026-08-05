package com.group3.vitamins.employee.application.port;

/**
 * 등록·수정 시 사원이 참조하는 다른 애그리게이트(부서·직급)의 존재를 확인하는 아웃바운드 포트
 * (`employee.md` §3·§4). 소비자(employee)가 소유하는 조회 포트이며 MyBatis 어댑터가 구현한다 (아키텍처 §2-1).
 */
public interface EmployeeReferenceQueryPort {

    /** 부서가 존재하는가 (없으면 {@code EMP_DEPARTMENT_NOT_FOUND}). */
    boolean departmentExists(Long departmentId);

    /** 직급이 존재하는가 (없으면 {@code EMP_JOB_POSITION_NOT_FOUND}). */
    boolean jobPositionExists(Long jobPositionId);
}

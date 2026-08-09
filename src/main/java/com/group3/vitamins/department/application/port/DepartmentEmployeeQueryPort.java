package com.group3.vitamins.department.application.port;

import com.group3.vitamins.department.application.result.DepartmentEmployeeCountRow;

import java.util.List;

/**
 * 부서 조회 중 <b>{@code employee} 를 가로지르는</b> 집계를 위한 아웃바운드 포트.
 * 인원 집계는 사원 도메인 소관 테이블({@code employee})에 걸리므로 도메인 리포지토리(JPA)가 아니라
 * 이 포트로 분리하고, 실제 조회는 {@code infrastructure/adapter} 의 MyBatis 어댑터가 처리한다
 * (businesscategory 의 {@code ProjectCategoryLinkPort} 선례).
 */
public interface DepartmentEmployeeQueryPort {

    /**
     * 회사 범위 전체 부서를 직속 인원 수와 함께 조회한다 (정렬 {@code department_id} 오름차순 = 생성 순).
     * 인원 집계에서 시스템 계정·퇴사자는 제외한다 (`.ai/api/department.md` §1).
     */
    List<DepartmentEmployeeCountRow> findAllWithDirectEmployeeCount(Long companyId);

    /**
     * 부서 1건의 직속 사원 수 — 삭제 차단({@code DEPT_HAS_EMPLOYEES}) 판정용.
     * 목록의 집계와 같은 기준(시스템 계정·퇴사자 제외)을 쓴다.
     */
    long countDirectEmployees(Long departmentId);
}

package com.group3.vitamins.department.application.result;

import java.util.List;

/**
 * 부서 트리 노드 결과 (`.ai/api/department.md` §1). 계층은 최대 2단이라 {@code children} 의 원소는
 * 다시 자식을 갖지 않는다(항상 빈 배열). 서비스가 평면 조회 결과를 이 트리로 조립한다.
 *
 * @param departmentId        부서 번호
 * @param name                부서명
 * @param directEmployeeCount 직속 사원 수
 * @param totalEmployeeCount  하위 포함 사원 수
 * @param children            하위 부서 (없으면 빈 배열)
 */
public record DepartmentTreeResult(
        Long departmentId,
        String name,
        int directEmployeeCount,
        int totalEmployeeCount,
        List<DepartmentTreeResult> children
) {
}

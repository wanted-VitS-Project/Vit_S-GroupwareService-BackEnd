package com.group3.vitamins.department.application.result;

/**
 * 부서 생성·수정 결과 (`.ai/api/department.md` §2·§3).
 *
 * <p>생성은 인원 수가 항상 0 이고, 수정은 인원 수를 응답에 싣지 않는다(수정 응답 DTO 가 무시).
 * 두 유스케이스를 하나의 결과로 모으고, 응답 변환에서 필요한 필드만 골라 쓴다.
 *
 * @param departmentId        부서 번호
 * @param name                부서명
 * @param parentId            상위 부서 번호 ({@code null} 이면 최상위)
 * @param parentName          상위 부서명 ({@code null} 이면 최상위)
 * @param directEmployeeCount 직속 사원 수
 * @param totalEmployeeCount  하위 포함 사원 수
 */
public record DepartmentResult(
        Long departmentId,
        String name,
        Long parentId,
        String parentName,
        int directEmployeeCount,
        int totalEmployeeCount
) {
}

package com.group3.vitamins.department.application.result;

/**
 * 부서 1건 + 직속 인원 수 (트리 조립용 플랫 행). {@code employee} 조인으로 <b>직속</b> 인원만 센 결과다.
 *
 * <p>{@link com.group3.vitamins.department.application.port.DepartmentEmployeeQueryPort} 의 반환 타입이다.
 * 하위 포함 합계({@code totalEmployeeCount})는 여기 담지 않고 서비스가 트리를 조립하며 계산한다 —
 * 계층이 최대 2단이라 부모의 합계는 "자기 직속 + 자식들 직속" 이면 충분하다.
 *
 * @param departmentId        부서 번호
 * @param name                부서명
 * @param parentId            상위 부서 ({@code null} 이면 최상위)
 * @param directEmployeeCount 직속 사원 수 (시스템 계정·퇴사자 제외)
 */
public record DepartmentEmployeeCountRow(
        Long departmentId,
        String name,
        Long parentId,
        int directEmployeeCount
) {
}

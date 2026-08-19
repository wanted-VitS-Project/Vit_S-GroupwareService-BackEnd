package com.group3.vitamins.employee.application.query;

/**
 * 사원 목록 조회의 <b>SQL 실행 조건</b> — 서비스가 {@link EmployeeListQuery} 를 검증·변환한 결과.
 *
 * <p>화면용 {@code status}(ACTIVE·RESET_REQUIRED·INACTIVE)를 계정 상태와 비밀번호 변경 여부 두 축으로
 * 풀어 담는다. 이렇게 나눠두면 XML 은 단순 동등 비교만 하면 되고 조합 규칙(비즈니스)이 SQL 로 새지 않는다
 * (팀 MyBatis 컨벤션 — XML 내 비즈니스 로직 금지).
 *
 * @param keyword            이름·사번 부분 검색 (null 이면 미적용)
 * @param departmentId       부서 필터 (null 이면 미적용)
 * @param includeSubDepartments true 면 departmentId + 직속 하위 부서까지(트리 2단) 조회. departmentId 가 null 이면 무의미
 * @param role               MASTER·MEMBER 필터 (null 이면 미적용)
 * @param accountStatus      계정 상태 필터 ACTIVE·INACTIVE (null 이면 미적용)
 * @param mustChangePassword 비밀번호 변경 필요 여부 필터 (null 이면 미적용)
 * @param resignedOnly       true 면 퇴사자만, false 면 재직자만
 * @param offset             LIMIT 시작 위치 (page * size)
 * @param limit              페이지 크기
 * @param companyId          현재 회사 — 이 회사 사원만 조회한다(타사 사원 노출 차단)
 */
public record EmployeeListCriteria(
        String keyword,
        Long departmentId,
        boolean includeSubDepartments,
        String role,
        String accountStatus,
        Boolean mustChangePassword,
        boolean resignedOnly,
        int offset,
        int limit,
        Long companyId
) {
}

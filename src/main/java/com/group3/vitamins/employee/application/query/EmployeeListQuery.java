package com.group3.vitamins.employee.application.query;

/**
 * 사원 목록 조회 요청 (`employee.md` §1). 컨트롤러가 받은 <b>가공 전</b> 파라미터를 그대로 담는다.
 *
 * <p>필터 값 검증(허용 role·status)과 화면용 {@code status} → (계정상태 · 비밀번호변경) 변환은
 * {@code EmployeeAdminQueryService} 가 한다. 여기서는 값을 나르기만 한다.
 *
 * @param requesterRole 요청자 전역 권한 (ADMIN 판정용)
 * @param keyword       이름 또는 사번 부분 검색 (null 허용)
 * @param departmentId  부서 필터 (null 허용)
 * @param includeSubDepartments true 면 departmentId 의 하위 부서 사원까지 포함 (departmentId 없으면 무시)
 * @param role          {@code MASTER} · {@code MEMBER} 필터 (null 허용)
 * @param status        {@code ACTIVE} · {@code RESET_REQUIRED} · {@code INACTIVE} 필터 (null 허용)
 * @param resigned      퇴사 여부. null 이면 재직자만
 * @param page          0-base 페이지 번호
 * @param size          페이지 크기
 */
public record EmployeeListQuery(
        String requesterRole,
        String keyword,
        Long departmentId,
        boolean includeSubDepartments,
        String role,
        String status,
        Boolean resigned,
        int page,
        int size
) {
}

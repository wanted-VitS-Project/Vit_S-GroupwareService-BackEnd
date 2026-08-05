package com.group3.vitamins.employee.application.command;

/**
 * 사원 등록 커맨드 (`employee.md` §3). 컨트롤러가 받은 <b>가공 전</b> 값을 담는다 —
 * role 허용값 검증, {@code hiredAt} 파싱, 필수값 검증은 {@code EmployeeCommandService} 가 한다
 * (도메인 에러코드로 던지기 위해 `@NotBlank` 등 프레임워크 검증을 쓰지 않는다 — 아키텍처 §4).
 *
 * @param actorRole    요청자 전역 권한 (ADMIN 판정용)
 * @param userId       사번 = 로그인 아이디 (필수)
 * @param name         이름 (필수)
 * @param departmentId 부서 ID (필수)
 * @param hiredAt      입사일 {@code yyyy-MM-dd} (필수)
 * @param role         전역 권한 {@code MASTER}·{@code MEMBER} (필수, ADMIN 불가)
 * @param jobPositionId 직급 ID (선택)
 * @param email        초기 비밀번호 발송 주소 (선택 — 없으면 계정은 만들되 로그인 불가)
 * @param phone        연락처 (선택)
 */
public record RegisterEmployeeCommand(
        String actorRole,
        String userId,
        String name,
        Long departmentId,
        String hiredAt,
        String role,
        Long jobPositionId,
        String email,
        String phone
) {
}

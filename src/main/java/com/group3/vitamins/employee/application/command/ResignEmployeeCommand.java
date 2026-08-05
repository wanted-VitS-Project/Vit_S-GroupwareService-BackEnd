package com.group3.vitamins.employee.application.command;

/**
 * 퇴사 처리 커맨드 (`employee.md` §5).
 *
 * @param actorRole  요청자 전역 권한 (ADMIN 판정용)
 * @param userId     퇴사 대상 사번 (경로 변수)
 * @param resignedAt 퇴사일 {@code yyyy-MM-dd} (필수)
 */
public record ResignEmployeeCommand(
        String actorRole,
        String userId,
        String resignedAt
) {
}

package com.group3.vitamins.employee.application.result;

/**
 * 퇴사 처리 결과 (`employee.md` §5). 응답 필드 {@code userId·resignedAt·accountStatus} 그대로.
 * {@code accountStatus} 는 항상 {@code INACTIVE}(퇴사 시 계정이 함께 비활성화된다).
 */
public record EmployeeResignResult(
        String userId,
        String resignedAt,
        String accountStatus
) {
}

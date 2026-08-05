package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.EmployeeResignResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 퇴사 처리 응답 (`employee.md` §5). {@code userId·resignedAt·accountStatus(INACTIVE)}.
 */
public record EmployeeResignResponse(
        @Schema(description = "사번", example = "EMP021")
        String userId,
        @Schema(description = "퇴사일 yyyy-MM-dd", example = "2026-08-31")
        String resignedAt,
        @Schema(description = "계정 상태 (퇴사로 비활성화됨)", example = "INACTIVE")
        String accountStatus
) {

    public static EmployeeResignResponse from(EmployeeResignResult result) {
        return new EmployeeResignResponse(result.userId(), result.resignedAt(), result.accountStatus());
    }
}

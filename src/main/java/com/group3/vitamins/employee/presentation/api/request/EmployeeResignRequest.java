package com.group3.vitamins.employee.presentation.api.request;

import com.group3.vitamins.employee.application.command.ResignEmployeeCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 퇴사 처리 요청 (`employee.md` §5). 날짜 형식 검증은 서비스에서 도메인 에러코드로 한다.
 */
public record EmployeeResignRequest(
        @Schema(description = "퇴사일 yyyy-MM-dd", example = "2026-08-31")
        String resignedAt
) {

    public ResignEmployeeCommand toCommand(String actorRole, String userId) {
        return new ResignEmployeeCommand(actorRole, userId, resignedAt);
    }
}

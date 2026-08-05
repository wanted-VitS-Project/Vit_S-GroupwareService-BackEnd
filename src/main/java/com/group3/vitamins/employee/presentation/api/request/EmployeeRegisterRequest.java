package com.group3.vitamins.employee.presentation.api.request;

import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사원 등록 요청 (`employee.md` §3). 값 검증은 서비스에서 도메인 에러코드로 한다
 * (`@NotBlank` 등 프레임워크 검증은 명세 코드가 아니라 COMMON_* 을 내보내므로 쓰지 않는다 — 아키텍처 §4).
 */
public record EmployeeRegisterRequest(
        @Schema(description = "사번(로그인 아이디)", example = "EMP021")
        String userId,
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "부서 ID", example = "2")
        Long departmentId,
        @Schema(description = "입사일 yyyy-MM-dd", example = "2026-08-05")
        String hiredAt,
        @Schema(description = "전역 권한 (MASTER·MEMBER, ADMIN 불가)", example = "MEMBER")
        String role,
        @Schema(description = "직급 ID (선택)", example = "10")
        Long jobPositionId,
        @Schema(description = "초기 비밀번호를 보낼 이메일 (선택)", example = "hong@vitamins.com")
        String email,
        @Schema(description = "연락처 (선택)", example = "010-1234-5678")
        String phone
) {

    public RegisterEmployeeCommand toCommand(String actorRole) {
        return new RegisterEmployeeCommand(
                actorRole, userId, name, departmentId, hiredAt, role, jobPositionId, email, phone);
    }
}

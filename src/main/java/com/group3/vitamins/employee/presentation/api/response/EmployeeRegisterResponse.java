package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.EmployeeRegisterResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사원 등록 응답 (`employee.md` §3). {@code userId·name·emailRegistered·emailSent}.
 */
public record EmployeeRegisterResponse(
        @Schema(description = "사번", example = "EMP021")
        String userId,
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "이메일 등록 여부. false 면 초기 비밀번호를 전달할 수 없어 로그인 불가", example = "true")
        boolean emailRegistered,
        @Schema(description = "초기 비밀번호 메일 발송 성공 여부. false 면 재설정으로 재발송 필요", example = "true")
        boolean emailSent
) {

    public static EmployeeRegisterResponse from(EmployeeRegisterResult result) {
        return new EmployeeRegisterResponse(
                result.userId(), result.name(), result.emailRegistered(), result.emailSent());
    }
}

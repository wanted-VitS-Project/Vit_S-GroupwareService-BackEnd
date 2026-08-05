package com.group3.vitamins.auth.presentation.api.request;

import com.group3.vitamins.auth.application.command.LoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(

        @Schema(description = "사번. 로그인 아이디로 사용한다", example = "EMP001")
        @NotBlank(message = "사번을 입력해 주세요.")
        String userId,

        @Schema(description = "비밀번호")
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {

    /** 요청을 서비스 커맨드로 옮긴다. */
    public LoginCommand toCommand() {
        return new LoginCommand(userId, password);
    }
}

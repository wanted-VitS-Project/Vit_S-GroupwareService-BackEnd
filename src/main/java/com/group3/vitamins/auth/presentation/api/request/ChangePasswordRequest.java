package com.group3.vitamins.auth.presentation.api.request;

import com.group3.vitamins.auth.application.command.ChangePasswordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 변경 요청")
public record ChangePasswordRequest(

        @Schema(description = "현재 비밀번호. 최초 변경(passwordStatus=RESET_REQUIRED)일 때만 생략 가능")
        String currentPassword,

        @Schema(description = "새 비밀번호. 8자 이상 + 영문·숫자·특수문자 모두 포함")
        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        String newPassword,

        @Schema(description = "새 비밀번호 확인. 서버에서도 일치를 검증한다")
        @NotBlank(message = "새 비밀번호 확인을 입력해 주세요.")
        String newPasswordConfirm
) {

    /** 요청을 서비스 커맨드로 옮긴다. {@code userId} 는 세션에서 온다. */
    public ChangePasswordCommand toCommand(String userId) {
        return new ChangePasswordCommand(userId, currentPassword, newPassword, newPasswordConfirm);
    }
}

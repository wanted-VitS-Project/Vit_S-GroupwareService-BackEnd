package com.group3.vitamins.account.presentation.api.request;

import com.group3.vitamins.account.application.command.ResetPasswordsCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 비밀번호 재설정 요청 (`.ai/api/account.md` §3).
 *
 * <p>개인·다중 재설정이 같은 API 다. 1명이면 길이 1 배열이다 (ACC-007·013).
 * 빈 배열 검증은 서비스에서 {@code ACC_INVALID_REQUEST} 로 한다.
 */
public record ResetPasswordRequest(
        @Schema(description = "대상 사번 목록 (1개 이상)", example = "[\"EMP001\", \"EMP002\"]")
        List<String> userIds
) {

    /** 요청을 서비스 커맨드로 옮긴다. 요청자 권한은 세션에서 온다. */
    public ResetPasswordsCommand toCommand(String actorRole) {
        return new ResetPasswordsCommand(actorRole, userIds);
    }
}

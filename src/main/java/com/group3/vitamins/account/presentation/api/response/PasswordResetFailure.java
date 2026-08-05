package com.group3.vitamins.account.presentation.api.response;

import com.group3.vitamins.account.application.result.PasswordResetFailureResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 비밀번호 재설정 실패 1건 (`.ai/api/account.md` §3 — {@code data.failures[]}).
 *
 * <p>도메인 실패 사유를 문자열과 {@code passwordChanged} 로 펼쳐 프론트에 내려준다.
 */
public record PasswordResetFailure(
        @Schema(description = "사번", example = "EMP003")
        String userId,
        @Schema(description = "이름", example = "박지훈")
        String name,
        @Schema(description = "실패 사유", allowableValues = {"EMAIL_NOT_REGISTERED", "MAIL_SEND_FAILED"})
        String reason,
        @Schema(description = "비밀번호가 실제로 바뀌었는지. true 면 반드시 재시도해야 한다", example = "false")
        boolean passwordChanged
) {

    /** application 결과를 응답 항목으로 옮긴다. */
    public static PasswordResetFailure from(PasswordResetFailureResult result) {
        return new PasswordResetFailure(
                result.userId(),
                result.name(),
                result.reason().name(),
                result.reason().isPasswordChanged());
    }
}

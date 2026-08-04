package com.group3.vitamins.account.presentation.api.dto.response;

import com.group3.vitamins.account.domain.PasswordResetFailureReason;
import com.group3.vitamins.account.infrastructure.persistence.AccountTargetRow;
import io.swagger.v3.oas.annotations.media.Schema;

/** 비밀번호 재설정 실패 1건 (`.ai/api/account.md` §3 — {@code data.failures[]}) */
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

    public static PasswordResetFailure of(AccountTargetRow target, PasswordResetFailureReason reason) {
        return new PasswordResetFailure(
                target.userId(), target.name(), reason.name(), reason.isPasswordChanged());
    }
}

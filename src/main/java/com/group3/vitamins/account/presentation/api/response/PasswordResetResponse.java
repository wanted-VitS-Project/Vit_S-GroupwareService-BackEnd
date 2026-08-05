package com.group3.vitamins.account.presentation.api.response;

import com.group3.vitamins.account.application.result.PasswordResetResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 비밀번호 재설정 결과 집계 (`.ai/api/account.md` §3).
 *
 * <p>실패가 섞여 있어도 HTTP 200 이다 — 프론트가 집계를 그대로 보여줘야 하기 때문이다.
 */
public record PasswordResetResponse(
        @Schema(description = "요청 건수", example = "3")
        int requestedCount,
        @Schema(description = "성공 건수", example = "1")
        int successCount,
        @Schema(description = "실패 건수", example = "2")
        int failedCount,
        @Schema(description = "실패 목록")
        List<PasswordResetFailure> failures
) {

    /** application 결과를 응답으로 옮긴다. */
    public static PasswordResetResponse from(PasswordResetResult result) {
        return new PasswordResetResponse(
                result.requestedCount(),
                result.successCount(),
                result.failedCount(),
                result.failures().stream()
                        .map(PasswordResetFailure::from)
                        .toList());
    }
}

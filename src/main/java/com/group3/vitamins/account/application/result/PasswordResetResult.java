package com.group3.vitamins.account.application.result;

import java.util.List;

/**
 * 비밀번호 재설정 결과 집계 (application 결과, `.ai/api/account.md` §3).
 *
 * <p>실패가 섞여 있어도 요청 자체는 성공(HTTP 200)이다 — 프론트가 집계를 그대로 보여줘야 하기 때문이다.
 *
 * @param requestedCount 요청 건수 (중복 제거 후)
 * @param successCount   메일 발송까지 성공한 건수
 * @param failedCount    실패 건수
 * @param failures       실패 목록
 */
public record PasswordResetResult(
        int requestedCount,
        int successCount,
        int failedCount,
        List<PasswordResetFailureResult> failures
) {
}

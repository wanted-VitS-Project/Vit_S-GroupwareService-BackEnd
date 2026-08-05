package com.group3.vitamins.account.application.command;

import java.util.List;

/**
 * 비밀번호 일괄 재설정 커맨드 (`.ai/api/account.md` §3).
 *
 * @param actorRole 요청자 전역 권한 — ADMIN 판정에 쓴다
 * @param userIds   대상 사번 목록 (1개 이상. 빈 배열 검증은 서비스에서 한다)
 */
public record ResetPasswordsCommand(
        String actorRole,
        List<String> userIds
) {
}

package com.group3.vitamins.auth.application.command;

/**
 * 로그인 커맨드 (`.ai/api/auth.md` §1).
 *
 * @param userId      사번 (로그인 아이디)
 * @param rawPassword 평문 비밀번호 — 로그에 남기지 않는다
 */
public record LoginCommand(
        String userId,
        String rawPassword
) {
}

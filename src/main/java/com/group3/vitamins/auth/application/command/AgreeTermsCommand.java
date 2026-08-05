package com.group3.vitamins.auth.application.command;

/**
 * 약관 동의 커맨드 (`.ai/api/auth.md` §5). 최초 로그인 시 1회. 요청 본문이 없어 세션 사번만 담는다.
 *
 * @param userId 세션에서 온 본인 사번
 */
public record AgreeTermsCommand(
        String userId
) {
}

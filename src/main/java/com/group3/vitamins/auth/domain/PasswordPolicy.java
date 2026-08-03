package com.group3.vitamins.auth.domain;

import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;

/**
 * 비밀번호 정책 — 3개 모두 필수 (`.ai/api/auth.md` §4, 2026-08-03 확정).
 *
 * <ul>
 *   <li>8자 이상</li>
 *   <li>영문·숫자 포함</li>
 *   <li>특수문자 포함</li>
 * </ul>
 *
 * <p>정규식 하나로 몰아넣지 않은 이유 — 어떤 조건에서 걸렸는지 로그로 알 수 있어야 하고,
 * 조건이 늘거나 줄 때 읽기 쉬워야 한다.
 */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new ValidationException(AuthErrorCode.AUTH_PASSWORD_POLICY_VIOLATION);
        }
        if (!containsLetter(rawPassword) || !containsDigit(rawPassword) || !containsSpecial(rawPassword)) {
            throw new ValidationException(AuthErrorCode.AUTH_PASSWORD_POLICY_VIOLATION);
        }
    }

    /**
     * 명세는 <b>"영문"</b> 이다. {@code Character.isLetter} 를 쓰면 한글도 통과하므로
     * ASCII 영문자로 한정한다 (한글만 든 비밀번호가 정책을 만족해버린다).
     */
    private static boolean containsLetter(String value) {
        return value.chars().anyMatch(ch -> (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'));
    }

    private static boolean containsDigit(String value) {
        return value.chars().anyMatch(Character::isDigit);
    }

    /** 영문·숫자·공백이 아닌 출력 가능 문자를 특수문자로 본다. 허용 목록을 열거하면 빠뜨리는 문자가 생긴다. */
    private static boolean containsSpecial(String value) {
        return value.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch));
    }
}

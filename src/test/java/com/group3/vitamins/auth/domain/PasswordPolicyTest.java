package com.group3.vitamins.auth.domain;

import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 비밀번호 정책 — 8자 이상 + 영문·숫자·특수문자 모두 (`.ai/api/auth.md` §4) */
@DisplayName("PasswordPolicy")
class PasswordPolicyTest {

    @Test
    @DisplayName("null 은 거부한다")
    void rejectsNull() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null))
                .isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest(name = "거부: \"{0}\"")
    @ValueSource(strings = {
            "Ab1!",             // 8자 미만
            "Ab1!567",          // 7자
            "abcdefgh",         // 숫자·특수문자 없음
            "abcdefg1",         // 특수문자 없음
            "abcdefg!",         // 숫자 없음
            "1234567!",         // 영문 없음
            "가나다라1234!"       // 한글은 '영문' 이 아니다
    })
    @DisplayName("조건을 하나라도 못 채우면 AUTH_PASSWORD_POLICY_VIOLATION")
    void rejectsPolicyViolation(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getErrorCode())
                        .isEqualTo(AuthErrorCode.AUTH_PASSWORD_POLICY_VIOLATION));
    }

    @ParameterizedTest(name = "허용: \"{0}\"")
    @ValueSource(strings = {
            "abcdefg1!",
            "Vit-S!2026",
            "P@ssw0rd",
            "한글도 섞이면 ok1!"   // 영문·숫자·특수가 모두 있으면 다른 문자가 섞여도 된다
    })
    @DisplayName("3개 조건을 모두 채우면 통과한다")
    void acceptsValidPassword(String password) {
        assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
    }
}

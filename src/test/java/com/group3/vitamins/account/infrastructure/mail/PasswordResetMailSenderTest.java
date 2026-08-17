package com.group3.vitamins.account.infrastructure.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@DisplayName("PasswordResetMailSender 본문 — 로그인 아이디(사번)를 함께 안내한다")
class PasswordResetMailSenderTest {

    private final JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    // 도메인 없는 불투명 값 — PUBLIC 레포라 실제처럼 보이는 주소를 커밋하지 않는다.
    private static final String LOGIN_URL = "login-url-placeholder";
    private final PasswordResetMailSender sender =
            new PasswordResetMailSender(mailSender, "mail-sender", LOGIN_URL);

    @Test
    @DisplayName("본문에 사번·이름·임시 비밀번호가 모두 담긴다")
    void bodyContainsUserIdAndPassword() {
        sender.sendTempPassword("recipient-address", "vitas-EMP001", "홍길동", "RAW-PW-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        String body = captor.getValue().getText();
        assertThat(body)
                .contains("vitas-EMP001")   // 로그인 아이디(사번) — 이번에 추가된 핵심
                .contains("홍길동")
                .contains("RAW-PW-123")
                .contains(LOGIN_URL);
    }
}

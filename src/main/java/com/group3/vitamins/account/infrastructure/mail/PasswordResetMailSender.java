package com.group3.vitamins.account.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 관리자 재설정 시 임시 비밀번호를 사원 이메일로 발송한다.
 *
 * <p>발송 실패는 {@link MailDeliveryException} 으로 던진다 — 호출부(재설정 유스케이스)가 이를 잡아
 * 명세의 {@code MAIL_SEND_FAILED}(passwordChanged=true) 로 집계한다. 이미 비밀번호는 바뀐 상태다.
 *
 * <p>⚠️ PUBLIC 레포다. SMTP 계정·앱 비밀번호 같은 실제 값은 커밋 파일에 두지 않는다 —
 * {@code spring.mail.*} 는 환경변수로 주입한다 (`application.yml` 참고).
 */
@Slf4j
@Component
public class PasswordResetMailSender {

    private final JavaMailSender mailSender;
    private final String from;
    private final String loginUrl;

    public PasswordResetMailSender(
            JavaMailSender mailSender,
            @Value("${account.mail.from:${spring.mail.username:no-reply@vitamins.app}}") String from,
            @Value("${account.mail.login-url:http://localhost:3000/login}") String loginUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.loginUrl = loginUrl;
    }

    /**
     * @param toEmail     수신 이메일 (호출부가 존재를 이미 확인했다)
     * @param name        사원 이름 (인사말)
     * @param rawPassword 평문 임시 비밀번호. 로그에 남기지 않는다
     */
    public void sendTempPassword(String toEmail, String name, String rawPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("[VitaminS] 임시 비밀번호가 발급되었습니다");
        message.setText(buildBody(name, rawPassword));

        try {
            mailSender.send(message);
            log.info("임시 비밀번호 메일 발송 완료 — to={}", maskEmail(toEmail));
        } catch (RuntimeException e) {
            // 원인은 로그로 남기고, 상위에는 원문 비밀번호가 딸려가지 않는 전용 예외로 던진다.
            log.warn("임시 비밀번호 메일 발송 실패 — to={}", maskEmail(toEmail), e);
            throw new MailDeliveryException(e);
        }
    }

    private String buildBody(String name, String rawPassword) {
        return """
                %s님, 안녕하세요.

                관리자에 의해 임시 비밀번호가 발급되었습니다.

                  임시 비밀번호: %s

                아래 주소로 로그인한 뒤 반드시 새 비밀번호로 변경해 주세요.
                  %s

                본인이 요청하지 않았다면 관리자에게 문의해 주세요.
                """.formatted(name, rawPassword, loginUrl);
    }

    /** 로그용 마스킹 — {@code ab***@domain} */
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}

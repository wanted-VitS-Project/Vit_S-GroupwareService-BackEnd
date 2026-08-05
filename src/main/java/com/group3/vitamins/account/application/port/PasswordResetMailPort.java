package com.group3.vitamins.account.application.port;

/**
 * 임시 비밀번호 메일 발송 아웃바운드 포트. 애플리케이션은 이 인터페이스만 알고, 실제 발송(SMTP)은
 * {@code infrastructure/mail} 어댑터가 처리한다 — 유스케이스가 메일 기술 구현과 분리된다.
 */
public interface PasswordResetMailPort {

    /**
     * 임시 비밀번호를 사원 이메일로 발송한다.
     *
     * <p>발송 실패는 {@link MailDeliveryException}(RuntimeException)으로 던진다 — 호출부(재설정 유스케이스)가
     * 이를 잡아 명세의 {@code MAIL_SEND_FAILED}(passwordChanged=true)로 집계한다. 이미 비밀번호는 바뀐 상태다.
     *
     * @param toEmail     수신 이메일 (호출부가 존재를 이미 확인했다)
     * @param name        사원 이름 (인사말)
     * @param rawPassword 평문 임시 비밀번호. 로그에 남기지 않는다
     */
    void sendTempPassword(String toEmail, String name, String rawPassword);
}

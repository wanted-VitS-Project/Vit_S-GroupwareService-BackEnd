package com.group3.vitamins.account.application.port;

/**
 * 메일 발송 실패. 비밀번호 재설정에서 {@code MAIL_SEND_FAILED} 로 집계된다.
 *
 * <p>도메인 예외({@code DomainException})가 아니다 — 요청 전체를 실패시키는 것이 아니라
 * <b>부분 실패</b>로 응답에 담아야 하기 때문이다. 상위(유스케이스)에서 잡아 처리한다.
 *
 * <p>{@link PasswordResetMailPort} 의 발송 실패 계약이라 포트와 같은 계층(application)에 둔다 —
 * 유스케이스가 {@code infrastructure} 구체 예외에 의존하지 않게 한다.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(Throwable cause) {
        super("임시 비밀번호 메일 발송에 실패했습니다.", cause);
    }
}

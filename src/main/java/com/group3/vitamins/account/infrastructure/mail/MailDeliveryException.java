package com.group3.vitamins.account.infrastructure.mail;

/**
 * 메일 발송 실패. 비밀번호 재설정에서 {@code MAIL_SEND_FAILED} 로 집계된다.
 *
 * <p>도메인 예외({@code DomainException})가 아니다 — 요청 전체를 실패시키는 것이 아니라
 * <b>부분 실패</b>로 응답에 담아야 하기 때문이다. 상위에서 잡아 처리한다.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(Throwable cause) {
        super("임시 비밀번호 메일 발송에 실패했습니다.", cause);
    }
}

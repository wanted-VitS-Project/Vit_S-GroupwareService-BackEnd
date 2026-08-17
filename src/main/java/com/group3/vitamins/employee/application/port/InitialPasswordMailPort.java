package com.group3.vitamins.employee.application.port;

/**
 * 등록 시 초기 비밀번호를 사원 이메일로 발송하는 아웃바운드 포트 (`employee.md` §3).
 *
 * <p>발송 문구·경로는 비밀번호 재설정과 동일하므로 어댑터는 account 의 메일 발송을 재사용한다.
 * 발송 실패는 {@code account.application.port.MailDeliveryException}(RuntimeException)으로 던진다 —
 * 등록 유스케이스가 이를 잡아 {@code emailSent=false} 로 응답하되 등록 자체는 201 로 성공시킨다
 * (사원·계정은 이미 만들어졌고 비밀번호만 다시 보내면 된다).
 */
public interface InitialPasswordMailPort {

    /**
     * 초기 비밀번호를 발송한다.
     *
     * @param toEmail     수신 이메일 (호출부가 존재를 이미 확인했다)
     * @param userId      로그인 아이디(사번). 메일에 함께 안내한다 — 로그인 아이디가 사번이라 사용자가 이걸 알아야 로그인한다
     * @param name        사원 이름 (인사말)
     * @param rawPassword 평문 초기 비밀번호. 로그에 남기지 않는다
     */
    void sendInitialPassword(String toEmail, String userId, String name, String rawPassword);
}

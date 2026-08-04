package com.group3.vitamins.account.domain;

import lombok.Getter;

/**
 * 비밀번호 재설정의 부분 실패 사유 (`.ai/api/account.md` §3 · ACC-016·018).
 *
 * <p>🔑 사유마다 {@code passwordChanged} 가 정해져 있다 — 이게 프론트 재시도 판단의 핵심이다.
 * <ul>
 *   <li>{@link #EMAIL_NOT_REGISTERED} → 비밀번호를 <b>바꾸지 않았다</b>(false). 전달할 곳이 없어서다.
 *       이메일을 등록한 뒤 다시 시도하면 된다</li>
 *   <li>{@link #MAIL_SEND_FAILED} → 비밀번호를 <b>이미 바꿨다</b>(true). 사용자는 새 비밀번호를 모르는
 *       상태라 반드시 다시 호출해야 한다</li>
 * </ul>
 */
@Getter
public enum PasswordResetFailureReason {

    EMAIL_NOT_REGISTERED(false),
    MAIL_SEND_FAILED(true);

    private final boolean passwordChanged;

    PasswordResetFailureReason(boolean passwordChanged) {
        this.passwordChanged = passwordChanged;
    }
}

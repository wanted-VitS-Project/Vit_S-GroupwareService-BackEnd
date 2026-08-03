package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

/**
 * 계정 잠금 — {@code 423 Locked}.
 *
 * <p>🚨 <b>{@link ForbiddenException}(403) 과 합치지 마라.</b> 프론트는 이 둘을 다르게 처리한다.
 * <ul>
 *   <li>{@code 403 AUTH_ACCOUNT_INACTIVE} — 관리자가 풀어줘야 한다. 사용자가 할 수 있는 게 없다</li>
 *   <li>{@code 423 AUTH_ACCOUNT_LOCKED} — 시간이 지나면 자동으로 풀린다. 해제 시각을 안내한다</li>
 * </ul>
 * 같은 상태코드로 내보내면 화면이 두 상황을 구분하지 못한다.
 */
public class AccountLockedException extends DomainException {

    public AccountLockedException(ErrorCode errorCode) {
        super(errorCode);
    }

    /** 잠금 해제 시각처럼 상황별 메시지가 필요한 경우 (코드는 유지) */
    public AccountLockedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public int getHttpStatus() {
        return 423;
    }
}

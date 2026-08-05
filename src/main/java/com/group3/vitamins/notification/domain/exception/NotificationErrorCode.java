package com.group3.vitamins.notification.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * NOTIFICATION 도메인 에러 코드.
 *
 * <p>⛔ 코드·메시지는 프론트와의 계약이다 (`.ai/api/notification.md` · 노션 확정).
 * 임의로 추가·변경하지 마라 (`.ai/API.md` §0).
 */
@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    /** ACT-002·ACT-003·VIW-007 — 존재하지 않거나 이미 삭제된 알림, 또는 연결된 block 이 삭제된 경우 */
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "존재하지 않거나 이미 삭제된 알림입니다."),
    /** ACT-002 — 다른 사용자의 알림에 접근 시도 */
    NOTIFICATION_FORBIDDEN("NOTIFICATION_FORBIDDEN", "다른 사용자의 알림에는 접근할 수 없습니다.");

    private final String code;
    private final String message;
}

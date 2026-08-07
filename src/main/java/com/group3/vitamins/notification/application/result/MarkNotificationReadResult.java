package com.group3.vitamins.notification.application.result;

import com.group3.vitamins.notification.domain.model.Notification;

import java.time.LocalDateTime;

/**
 * ACT-006 — 개별 읽음 처리 결과.
 *
 * <p>{@code readAt} 은 <b>최초 읽음 시각</b>이다. 이미 읽은 알림을 다시 호출해도 덮어쓰지 않으므로,
 * 두 번째 호출은 첫 번째와 같은 값을 돌려준다(멱등).
 */
public record MarkNotificationReadResult(Long notificationId, LocalDateTime readAt) {

    public static MarkNotificationReadResult from(Notification notification) {
        return new MarkNotificationReadResult(notification.getNotificationId(), notification.getReadAt());
    }
}

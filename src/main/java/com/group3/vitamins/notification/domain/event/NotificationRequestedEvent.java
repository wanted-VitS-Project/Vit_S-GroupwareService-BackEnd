package com.group3.vitamins.notification.domain.event;

import com.group3.vitamins.global.domain.event.DomainEvent;

import java.util.Objects;

/**
 * GEN-001 — 알림은 이 이벤트 하나로 생성된다. 도메인마다 다른 이벤트 클래스를 만들지 않는다.
 *
 * <p>{@code notification} 도메인 소유 패키지에 둔다(`global` 아님, 결재 등 특정 도메인 패키지도 아님) —
 * 필드가 전부 범용이라 다른 도메인이 이 클래스에 의존해도 알림 도메인이 그 도메인을 알 필요는 없다(INV-02).
 */
public record NotificationRequestedEvent(
        String recipientUserId,
        String notificationType,
        String title,
        String message,
        Long blockId
) implements DomainEvent {

    public NotificationRequestedEvent {
        Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        Objects.requireNonNull(notificationType, "notificationType must not be null");
        Objects.requireNonNull(title, "title must not be null");

        if (recipientUserId.isBlank()) {
            throw new IllegalArgumentException("recipientUserId must not be blank");
        }
        if (notificationType.isBlank()) {
            throw new IllegalArgumentException("notificationType must not be blank");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }

    public static NotificationRequestedEvent of(
            String recipientUserId, String notificationType, String title, String message, Long blockId) {
        return new NotificationRequestedEvent(recipientUserId, notificationType, title, message, blockId);
    }
}

package com.group3.vitamins.notification.domain.event;

import com.group3.vitamins.global.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RT-002 — 알림 row 가 <b>실제로 저장된 뒤</b> 발행된다. 실시간 전송(SSE)의 트리거다.
 *
 * <p>{@link NotificationRequestedEvent}(= "알림을 만들어 달라")와 구분된다. 이건 "알림이 만들어졌다"는
 * 사실의 통보이며, 그래서 {@code notificationId} 가 채워져 있다.
 *
 * <p>⚠️ <b>이 이벤트가 따로 있는 이유가 곧 RT-002 다.</b> {@code NotificationRequestedEventListener} 는
 * {@code REQUIRES_NEW} 트랜잭션 안에서 돈다 — 그 안에서 바로 푸시하면 <b>아직 커밋 전</b>이라,
 * 클라이언트가 푸시를 받고 즉시 목록을 조회했을 때 그 알림이 안 보일 수 있다. 저장 트랜잭션의
 * {@code AFTER_COMMIT} 으로 한 단계 더 미루기 위해 이벤트를 한 번 더 거친다.
 *
 * <p>필드는 목록 항목(§1)과 같은 것만 담는다 — 이동 대상({@code targetType}/{@code targetId})은
 * 넣지 않는다. 이동 정보는 클릭 시점에 이동 대상 조회 API(§3)가 전담하기 때문이다.
 */
public record NotificationCreatedEvent(
        Long notificationId,
        String recipientUserId,
        String notificationType,
        String title,
        String message,
        LocalDateTime createdAt
) implements DomainEvent {

    public NotificationCreatedEvent {
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        Objects.requireNonNull(notificationType, "notificationType must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}

package com.group3.vitamins.notification.domain.event;

import com.group3.vitamins.global.domain.event.DomainEvent;

import java.util.Map;
import java.util.Objects;

/**
 * GEN-001 — 알림은 이 이벤트 하나로 생성된다. 도메인마다 다른 이벤트 클래스를 만들지 않는다.
 *
 * <p>{@code notification} 도메인 소유 패키지에 둔다(`global` 아님, 결재 등 특정 도메인 패키지도 아님) —
 * 필드가 전부 범용(문자열·숫자·Map)이라 다른 도메인이 이 클래스에 의존해도 알림 도메인이 그 도메인을
 * 알 필요는 없다(INV-02).
 *
 * <p>GEN-005 — 이동 대상({@code targetType}/{@code targetId})과 부가 식별값({@code targetContext})은
 * <b>발행하는 도메인이 직접</b> 채운다. 알림 도메인은 이 값을 해석하지 않고 그대로 저장한다.
 *
 * @param targetType    이동 대상 도메인 유형({@code "APPROVAL"} 등). 이동 대상이 없으면 {@code null}
 * @param targetId      이동 대상 PK. {@code targetType} 과 <b>함께 있거나 함께 없어야</b> 한다
 * @param targetContext 이동에 필요한 부가 식별값({@code {"revisionId": 56}}). 표시용 데이터는 넣지 않는다.
 *                      생성 시점 스냅샷이며 클릭 시점에 재조회하지 않는다(VIW-010)
 */
public record NotificationRequestedEvent(
        String recipientUserId,
        String notificationType,
        String title,
        String message,
        String targetType,
        Long targetId,
        Map<String, Object> targetContext
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
        // GEN-005 — 부분 입력 차단(DB ck_notification_target 제약과 동일 규칙을 발행 시점에 먼저 막는다)
        if ((targetType == null) != (targetId == null)) {
            throw new IllegalArgumentException(
                    "targetType and targetId must be both present or both absent");
        }
        if (targetType != null && targetType.isBlank()) {
            throw new IllegalArgumentException("targetType must not be blank");
        }

        targetContext = (targetContext == null || targetContext.isEmpty()) ? null : Map.copyOf(targetContext);
    }

    /** 이동 대상이 있는 알림 */
    public static NotificationRequestedEvent of(
            String recipientUserId, String notificationType, String title, String message,
            String targetType, Long targetId, Map<String, Object> targetContext) {
        return new NotificationRequestedEvent(recipientUserId, notificationType, title, message,
                targetType, targetId, targetContext);
    }

    /** 이동 대상이 없는 알림(시스템 공지 등) */
    public static NotificationRequestedEvent of(
            String recipientUserId, String notificationType, String title, String message) {
        return new NotificationRequestedEvent(recipientUserId, notificationType, title, message,
                null, null, null);
    }
}

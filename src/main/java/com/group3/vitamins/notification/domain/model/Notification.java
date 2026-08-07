package com.group3.vitamins.notification.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

public class Notification {

    private final Long notificationId;
    private final String userId;
    private final String notificationType;
    private final String title;
    private final String message;
    private final String targetType;
    private final Long targetId;
    private final Map<String, Object> targetContext;
    private LocalDateTime readAt;
    private LocalDateTime deletedAt;
    private final LocalDateTime createdAt;

    private Notification(Long notificationId, String userId, String notificationType,
                         String title, String message, String targetType, Long targetId,
                         Map<String, Object> targetContext, LocalDateTime readAt, LocalDateTime deletedAt,
                         LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetContext = targetContext;
        this.readAt = readAt;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
    }

    /** GEN-004 — 이벤트 리스너가 수신자 한 명당 한 행씩 만든다. 아직 저장되지 않아 ID 가 없다. */
    public static Notification create(String userId, String notificationType, String title, String message,
                                      String targetType, Long targetId,
                                      Map<String, Object> targetContext, LocalDateTime now) {
        return new Notification(null, userId, notificationType, title, message,
                targetType, targetId, targetContext, null, null, now);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Notification restore(Long notificationId, String userId, String notificationType,
                                       String title, String message, String targetType, Long targetId,
                                       Map<String, Object> targetContext, LocalDateTime readAt,
                                       LocalDateTime deletedAt, LocalDateTime createdAt) {
        return new Notification(notificationId, userId, notificationType, title, message,
                targetType, targetId, targetContext, readAt, deletedAt, createdAt);
    }

    /** VIW-001 — 요청자가 수신자 본인인지 확인한다. */
    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }

    /** VIW-006 — 이동 대상이 지정된 알림인지. 없으면 응답에서 {@code type=NONE} 이 된다. */
    public boolean hasTarget() {
        return targetType != null;
    }

    /** ACT-004 — 이미 읽었으면 시각을 덮어쓰지 않는다(최초 읽음 시각 보존). */
    public void markRead(LocalDateTime now) {
        if (readAt == null) {
            this.readAt = now;
        }
    }

    public Long getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getNotificationType() { return notificationType; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public Map<String, Object> getTargetContext() { return targetContext; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

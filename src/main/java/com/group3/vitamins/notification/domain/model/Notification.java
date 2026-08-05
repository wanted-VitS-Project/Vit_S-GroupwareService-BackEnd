package com.group3.vitamins.notification.domain.model;

import java.time.LocalDateTime;

public class Notification {

    private final Long notificationId;
    private final Long blockId;
    private final String userId;
    private final String notificationType;
    private final String title;
    private final String message;
    private LocalDateTime readAt;
    private LocalDateTime deletedAt;
    private final LocalDateTime createdAt;

    private Notification(Long notificationId, Long blockId, String userId, String notificationType,
                         String title, String message, LocalDateTime readAt, LocalDateTime deletedAt,
                         LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.blockId = blockId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.readAt = readAt;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
    }

    /** GEN-004 — 이벤트 리스너가 수신자 한 명당 한 행씩 만든다. 아직 저장되지 않아 ID 가 없다. */
    public static Notification create(String userId, String notificationType, String title, String message,
                                      Long blockId, LocalDateTime now) {
        return new Notification(null, blockId, userId, notificationType, title, message, null, null, now);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Notification restore(Long notificationId, Long blockId, String userId, String notificationType,
                                       String title, String message, LocalDateTime readAt,
                                       LocalDateTime deletedAt, LocalDateTime createdAt) {
        return new Notification(notificationId, blockId, userId, notificationType, title, message,
                readAt, deletedAt, createdAt);
    }

    /** VIW-001 — 요청자가 수신자 본인인지 확인한다. */
    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** ACT-004 — 이미 읽었으면 시각을 덮어쓰지 않는다(최초 읽음 시각 보존). */
    public void markRead(LocalDateTime now) {
        if (readAt == null) {
            this.readAt = now;
        }
    }

    /** ACT-001 — 논리 삭제. */
    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }

    public Long getNotificationId() { return notificationId; }
    public Long getBlockId() { return blockId; }
    public String getUserId() { return userId; }
    public String getNotificationType() { return notificationType; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

package com.group3.vitamins.notification.application.result;

import com.group3.vitamins.notification.domain.model.Notification;

import java.time.LocalDateTime;

public record NotificationResult(
        Long notificationId,
        Long blockId,
        String notificationType,
        String title,
        String message,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.getNotificationId(),
                notification.getBlockId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}

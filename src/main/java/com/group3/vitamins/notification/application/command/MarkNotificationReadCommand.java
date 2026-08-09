package com.group3.vitamins.notification.application.command;

public record MarkNotificationReadCommand(Long notificationId, String requesterId) {
}

package com.group3.vitamins.notification.application.result;

import com.group3.vitamins.notification.domain.model.NotificationPage;

import java.util.List;

public record NotificationPageResult(
        List<NotificationResult> content,
        long totalElements,
        int totalPages
) {

    public static NotificationPageResult from(NotificationPage page) {
        return new NotificationPageResult(
                page.content().stream().map(NotificationResult::from).toList(),
                page.totalElements(),
                page.totalPages());
    }
}

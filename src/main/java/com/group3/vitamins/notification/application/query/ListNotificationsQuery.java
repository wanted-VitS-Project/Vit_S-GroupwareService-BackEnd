package com.group3.vitamins.notification.application.query;

public record ListNotificationsQuery(
        String userId,
        String category,
        Boolean isRead,
        int page,
        int size
) {

    private static final int DEFAULT_SIZE = 10;

    /** 페이지·크기 기본값(0페이지·10건)을 강제하고, 빈 문자열 category 는 "필터 없음"으로 눕힌다. */
    public ListNotificationsQuery {
        category = (category == null || category.isBlank()) ? null : category.trim();
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
    }
}

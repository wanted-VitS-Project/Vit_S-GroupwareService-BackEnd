package com.group3.vitamins.notification.domain.model;

import java.util.List;

/** 페이징 결과. Spring Data {@code Page} 를 domain 계층까지 새어나가게 하지 않기 위한 자체 래퍼. */
public record NotificationPage(List<Notification> content, long totalElements, int totalPages) {
}

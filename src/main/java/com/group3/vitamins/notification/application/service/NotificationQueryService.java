package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.notification.application.query.ListNotificationsQuery;
import com.group3.vitamins.notification.application.result.NotificationPageResult;
import com.group3.vitamins.notification.application.usecase.NotificationQueryUseCase;
import com.group3.vitamins.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService implements NotificationQueryUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationPageResult listNotifications(ListNotificationsQuery query) {
        return NotificationPageResult.from(notificationRepository.search(
                query.userId(), query.category(), query.isRead(), query.page(), query.size()));
    }
}

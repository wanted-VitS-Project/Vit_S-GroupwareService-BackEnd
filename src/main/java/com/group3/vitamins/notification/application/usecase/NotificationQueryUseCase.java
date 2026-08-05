package com.group3.vitamins.notification.application.usecase;

import com.group3.vitamins.notification.application.query.ListNotificationsQuery;
import com.group3.vitamins.notification.application.result.NotificationPageResult;

public interface NotificationQueryUseCase {

    NotificationPageResult listNotifications(ListNotificationsQuery query);
}

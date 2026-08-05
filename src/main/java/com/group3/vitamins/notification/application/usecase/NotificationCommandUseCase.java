package com.group3.vitamins.notification.application.usecase;

import com.group3.vitamins.notification.application.command.DeleteNotificationCommand;
import com.group3.vitamins.notification.application.command.GetNotificationTargetCommand;
import com.group3.vitamins.notification.application.command.MarkAllReadCommand;
import com.group3.vitamins.notification.application.result.MarkAllReadResult;
import com.group3.vitamins.notification.application.result.NotificationTargetResult;

public interface NotificationCommandUseCase {

    void deleteNotification(DeleteNotificationCommand command);

    /** VIW-006~008 · ACT-004 — 이동 대상을 조회하면서 동시에 자동 읽음 처리한다. */
    NotificationTargetResult getTarget(GetNotificationTargetCommand command);

    MarkAllReadResult markAllRead(MarkAllReadCommand command);
}

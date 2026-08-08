package com.group3.vitamins.notification.application.usecase;

import com.group3.vitamins.notification.application.command.DeleteNotificationCommand;
import com.group3.vitamins.notification.application.command.GetNotificationTargetCommand;
import com.group3.vitamins.notification.application.command.MarkNotificationReadCommand;
import com.group3.vitamins.notification.application.result.MarkNotificationReadResult;
import com.group3.vitamins.notification.application.result.NotificationTargetResult;

public interface NotificationCommandUseCase {

    /** ACT-001~003 — 본인 알림을 논리 삭제한다. */
    void deleteNotification(DeleteNotificationCommand command);

    /** VIW-006~010 · ACT-004 — 이동 대상을 조회하면서 동시에 자동 읽음 처리한다. */
    NotificationTargetResult getTarget(GetNotificationTargetCommand command);

    /** ACT-006 — 이동 없이 읽음만 처리한다. 이미 읽었으면 최초 시각을 유지한다(멱등). */
    MarkNotificationReadResult markRead(MarkNotificationReadCommand command);
}

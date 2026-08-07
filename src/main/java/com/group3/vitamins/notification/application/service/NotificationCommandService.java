package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.notification.application.command.GetNotificationTargetCommand;
import com.group3.vitamins.notification.application.command.MarkAllReadCommand;
import com.group3.vitamins.notification.application.result.MarkAllReadResult;
import com.group3.vitamins.notification.application.result.NotificationTargetResult;
import com.group3.vitamins.notification.application.usecase.NotificationCommandUseCase;
import com.group3.vitamins.notification.domain.exception.NotificationErrorCode;
import com.group3.vitamins.notification.domain.model.Notification;
import com.group3.vitamins.notification.domain.repository.NotificationRepository;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationCommandService implements NotificationCommandUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationTargetResult getTarget(GetNotificationTargetCommand command) {
        Notification notification = getOwnedNotificationOrThrow(command.notificationId(), command.requesterId());

        NotificationTargetResult result = notification.hasTarget()
                ? new NotificationTargetResult(
                        notification.getTargetType(), notification.getTargetId(), notification.getTargetContext())
                : NotificationTargetResult.none();

        // ACT-004 — 조회 성공 시 자동 읽음 처리
        notification.markRead(LocalDateTime.now());
        notificationRepository.save(notification);

        return result;
    }

    @Override
    public MarkAllReadResult markAllRead(MarkAllReadCommand command) {
        int markedCount = notificationRepository.markAllRead(command.requesterId(), LocalDateTime.now());
        return new MarkAllReadResult(markedCount);
    }

    /**
     * VIW-009 — 알림 자체의 존재·소유권만 확인한다. <b>이동 대상이 실제로 존재하는지, 볼 권한이 있는지는
     * 검증하지 않는다</b> — 그건 실제 도메인 페이지 API 책임이다(결재는 {@code ApprovalViewPolicy}).
     * 그래서 {@code NOTIFICATION_NOT_FOUND} 는 알림이 없거나 삭제됐을 때만 발생한다.
     */
    private Notification getOwnedNotificationOrThrow(Long notificationId, String requesterId) {
        Notification notification = notificationRepository.findActiveById(notificationId)
                .orElseThrow(() -> new NotFoundException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isOwnedBy(requesterId)) {
            throw new ForbiddenException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
        }
        return notification;
    }
}

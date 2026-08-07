package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.notification.application.command.GetNotificationTargetCommand;
import com.group3.vitamins.notification.application.command.MarkAllReadCommand;
import com.group3.vitamins.notification.application.port.BlockRef;
import com.group3.vitamins.notification.application.port.BlockTypeLookupPort;
import com.group3.vitamins.notification.application.port.NotificationTarget;
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
    private final BlockTypeLookupPort blockTypeLookupPort;
    private final NotificationTargetResolverRegistry targetResolverRegistry;

    @Override
    public NotificationTargetResult getTarget(GetNotificationTargetCommand command) {
        Notification notification = getOwnedNotificationOrThrow(command.notificationId(), command.requesterId());

        NotificationTargetResult result = resolveTarget(notification);

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

    private Notification getOwnedNotificationOrThrow(Long notificationId, String requesterId) {
        Notification notification = notificationRepository.findActiveById(notificationId)
                .orElseThrow(() -> new NotFoundException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isOwnedBy(requesterId)) {
            throw new ForbiddenException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
        }
        return notification;
    }

    /** VIW-006~008 — block_id 가 없거나, block 이 삭제됐거나, 지원하지 않는 타입이면 전부 NONE(에러 아님). block 삭제만 404. */
    private NotificationTargetResult resolveTarget(Notification notification) {
        if (notification.getBlockId() == null) {
            return NotificationTargetResult.none();
        }

        BlockRef blockRef = blockTypeLookupPort.findBlock(notification.getBlockId())
                .orElseThrow(() -> new NotFoundException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        return targetResolverRegistry.find(blockRef.type())
                .flatMap(resolver -> resolver.resolve(blockRef.typeId()))
                .map(target -> toResult(blockRef.type(), target))
                .orElseGet(NotificationTargetResult::none);
    }

    private NotificationTargetResult toResult(String type, NotificationTarget target) {
        return new NotificationTargetResult(type, target.targetId(), target.extra());
    }
}

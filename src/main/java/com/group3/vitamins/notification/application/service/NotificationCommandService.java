package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.notification.application.command.DeleteNotificationCommand;
import com.group3.vitamins.notification.application.command.GetNotificationTargetCommand;
import com.group3.vitamins.notification.application.command.MarkNotificationReadCommand;
import com.group3.vitamins.notification.application.result.MarkNotificationReadResult;
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
    public void deleteNotification(DeleteNotificationCommand command) {
        Notification notification = getOwnedNotificationOrThrow(command.notificationId(), command.requesterId());

        notification.delete(LocalDateTime.now());
        notificationRepository.save(notification);

        log.info("알림 삭제 완료 - notificationId={}", command.notificationId());
    }

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

    /**
     * ACT-006 — 이미 읽었으면 최초 시각을 유지한다(도메인이 보장). 재호출해도 같은 값이 나간다.
     *
     * <p>⚠️ <b>동시 호출은 잠금으로 막지 않는다(의도된 선택).</b> 두 요청이 같은 알림을 동시에 읽으면
     * 둘 다 {@code readAt == null} 을 보고 각자 저장해, 나중 트랜잭션의 시각이 남을 수 있다.
     * 잃는 것은 <b>읽음 시각의 밀리초 단위 정확도</b>뿐이고(두 시각이 사실상 같다), 읽음 여부·목록 노출·
     * 삭제 같은 실제 동작에는 영향이 없다. 이걸 막으려면 {@code @Version} + 재시도나 비관적 잠금이
     * 필요한데, 그 복잡도가 얻는 것보다 크다고 판단했다. {@code getTarget()} 의 자동 읽음도 같다.
     */
    @Override
    public MarkNotificationReadResult markRead(MarkNotificationReadCommand command) {
        Notification notification = getOwnedNotificationOrThrow(command.notificationId(), command.requesterId());

        notification.markRead(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);

        return MarkNotificationReadResult.from(saved);
    }

    /**
     * VIW-009 — 알림 자체의 존재·소유권만 확인한다. <b>이동 대상이 실제로 존재하는지, 볼 권한이 있는지는
     * 검증하지 않는다</b> — 그건 실제 도메인 페이지 API 책임이다(결재는 {@code ApprovalViewPolicy}).
     * 그래서 {@code NOTIFICATION_NOT_FOUND} 는 알림이 없거나 삭제됐을 때만 발생한다.
     *
     * <p>삭제분을 먼저 걸러내므로(ACT-003) 이미 삭제된 알림은 타인 것인지 아닌지와 무관하게 404 다 —
     * 존재 여부가 드러나지 않는다.
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

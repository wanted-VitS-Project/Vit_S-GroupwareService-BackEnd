package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.notification.domain.event.NotificationCreatedEvent;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import com.group3.vitamins.notification.domain.model.Notification;
import com.group3.vitamins.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * GEN-002 — 발행 트랜잭션이 커밋된 후에만 알림을 저장한다. 롤백되면 알림도 생기지 않는다.
 *
 * <p>{@code AFTER_COMMIT} 시점엔 원래 트랜잭션이 이미 끝나 있어 {@code REQUIRES_NEW} 로 새 트랜잭션을 연다
 * (활동 로그의 {@code BEFORE_COMMIT}+{@code MANDATORY} 조합과 다른 이유 — 여긴 명세가 커밋 후 처리를 요구한다).
 */
@Component
@RequiredArgsConstructor
public class NotificationRequestedEventListener {

    private final NotificationRepository notificationRepository;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * ⚠️ <b>여기서 SSE 를 직접 보내지 마라.</b> 이 메서드는 {@code REQUIRES_NEW} 트랜잭션 안이라
     * 아직 커밋 전이다 — 이 자리에서 푸시하면 클라이언트가 알림을 받고 곧바로 목록을 조회했을 때
     * 그 알림이 <b>없는</b> 경우가 생긴다(레이스). 대신 {@link NotificationCreatedEvent} 를 발행해
     * 이 트랜잭션의 {@code AFTER_COMMIT} 으로 한 단계 미룬다 — 받는 쪽은
     * {@code NotificationPushEventListener} 다 (§5 RT-002).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationRequestedEvent event) {
        Notification saved = notificationRepository.save(Notification.create(
                event.recipientUserId(), event.notificationType(),
                event.title(), event.message(),
                event.targetType(), event.targetId(), event.targetContext(),
                LocalDateTime.now()));

        domainEventPublisher.publish(new NotificationCreatedEvent(
                saved.getNotificationId(), saved.getUserId(), saved.getNotificationType(),
                saved.getTitle(), saved.getMessage(), saved.getCreatedAt()));
    }
}

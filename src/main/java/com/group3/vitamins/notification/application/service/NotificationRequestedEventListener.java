package com.group3.vitamins.notification.application.service;

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationRequestedEvent event) {
        notificationRepository.save(Notification.create(
                event.recipientUserId(), event.notificationType(),
                event.title(), event.message(), event.blockId(), LocalDateTime.now()));
    }
}

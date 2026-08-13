package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.notification.application.port.NotificationPushPort;
import com.group3.vitamins.notification.application.result.NotificationResult;
import com.group3.vitamins.notification.domain.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RT-002 — 알림 저장이 <b>커밋된 뒤</b> 실시간 전송한다 (§5).
 *
 * <p>{@code AFTER_COMMIT} 이므로 이 시점엔 알림 row 가 확정돼 있다. 클라이언트가 푸시를 받고 바로
 * 목록을 조회해도 그 알림이 반드시 조회된다.
 *
 * <p>여기엔 트랜잭션을 열지 않는다({@code @Transactional} 없음) — DB 를 건드리지 않고 이벤트 필드만
 * 그대로 전달하기 때문이다. 커밋 후에 새 트랜잭션을 여는 것은 커넥션만 낭비한다.
 */
@Component
@RequiredArgsConstructor
public class NotificationPushEventListener {

    private final NotificationPushPort notificationPushPort;

    /**
     * RT-004 — 전송 실패를 여기서 처리하지 않는다. 어댑터가 삼키고 로그만 남긴다. 알림 row 는 이미
     * 저장돼 있어 다음 목록 조회에서 보이므로, 실시간 전송 실패가 알림 유실이 되지는 않는다.
     *
     * <p>{@code readAt} 을 {@code null} 로 고정하는 이유: 방금 만들어진 알림은 정의상 안 읽음이다
     * (읽음은 사용자 행동으로만 생긴다 — ACT-004 · ACT-006).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationCreatedEvent event) {
        notificationPushPort.push(event.recipientUserId(), new NotificationResult(
                event.notificationId(), event.notificationType(),
                event.title(), event.message(),
                null, event.createdAt()));
    }
}

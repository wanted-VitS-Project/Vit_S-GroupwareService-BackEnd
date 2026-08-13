package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.notification.domain.event.NotificationCreatedEvent;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import com.group3.vitamins.notification.domain.model.Notification;
import com.group3.vitamins.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRequestedEventListener — 저장 후 실시간 전송 트리거 발행 (RT-002)")
class NotificationRequestedEventListenerTest {

    private static final String RECIPIENT = "EMP003";
    private static final Long SAVED_ID = 301L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private NotificationRequestedEventListener listener;

    private final LocalDateTime createdAt = LocalDateTime.of(2026, 8, 12, 9, 0);

    @Test
    @DisplayName("알림을 저장하고, 저장된 ID 를 담아 NotificationCreatedEvent 를 발행한다")
    void 저장_후_생성_이벤트를_발행한다() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved());

        listener.handle(NotificationRequestedEvent.of(
                RECIPIENT, "APPROVAL_REQUESTED", "결재 요청", "출장비 정산 결재 요청이 도착했습니다.",
                "APPROVAL", 55L, Map.of("revisionId", 56L)));

        ArgumentCaptor<NotificationCreatedEvent> captor =
                ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());

        // ⚠️ notificationId 는 save() 가 돌려준 값이어야 한다. 발행 전 객체(ID null)를 그대로
        //    실어 보내면 NotificationCreatedEvent 생성자에서 NPE 로 터진다.
        assertThat(captor.getValue()).isEqualTo(new NotificationCreatedEvent(
                SAVED_ID, RECIPIENT, "APPROVAL_REQUESTED", "결재 요청",
                "출장비 정산 결재 요청이 도착했습니다.", createdAt));
    }

    @Test
    @DisplayName("이동 대상이 없는 알림도 동일하게 생성 이벤트를 발행한다")
    void 이동_대상이_없어도_발행한다() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(
                Notification.restore(SAVED_ID, RECIPIENT, "SYSTEM_NOTICE", "점검 안내", "02:00~04:00",
                        null, null, null, null, null, createdAt));

        listener.handle(NotificationRequestedEvent.of(
                RECIPIENT, "SYSTEM_NOTICE", "점검 안내", "02:00~04:00"));

        ArgumentCaptor<NotificationCreatedEvent> captor =
                ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());

        assertThat(captor.getValue().notificationId()).isEqualTo(SAVED_ID);
        assertThat(captor.getValue().recipientUserId()).isEqualTo(RECIPIENT);
    }

    private Notification saved() {
        return Notification.restore(SAVED_ID, RECIPIENT, "APPROVAL_REQUESTED", "결재 요청",
                "출장비 정산 결재 요청이 도착했습니다.",
                "APPROVAL", 55L, Map.of("revisionId", 56L), null, null, createdAt);
    }
}

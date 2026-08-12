package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.notification.application.port.NotificationPushPort;
import com.group3.vitamins.notification.application.result.NotificationResult;
import com.group3.vitamins.notification.domain.event.NotificationCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPushEventListener — 실시간 전송 (§5)")
class NotificationPushEventListenerTest {

    private static final String RECIPIENT = "EMP003";

    @Mock
    private NotificationPushPort notificationPushPort;

    @InjectMocks
    private NotificationPushEventListener listener;

    @Test
    @DisplayName("목록 항목과 같은 구조로 수신자에게 전송한다. 새 알림이므로 readAt 은 항상 null 이다")
    void 목록_항목과_같은_구조로_전송한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 12, 9, 0);

        listener.handle(new NotificationCreatedEvent(
                301L, RECIPIENT, "APPROVAL_REQUESTED", "결재 요청",
                "출장비 정산 결재 요청이 도착했습니다.", createdAt));

        ArgumentCaptor<NotificationResult> captor = ArgumentCaptor.forClass(NotificationResult.class);
        verify(notificationPushPort).push(eq(RECIPIENT), captor.capture());

        // 방금 생긴 알림은 정의상 안 읽음이다 — readAt 이 채워지면 프론트 배지가 안 올라간다
        assertThat(captor.getValue()).isEqualTo(new NotificationResult(
                301L, "APPROVAL_REQUESTED", "결재 요청",
                "출장비 정산 결재 요청이 도착했습니다.", null, createdAt));
    }
}

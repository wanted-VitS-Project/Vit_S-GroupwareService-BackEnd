package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.notification.application.command.DeleteNotificationCommand;
import com.group3.vitamins.notification.application.command.GetNotificationTargetCommand;
import com.group3.vitamins.notification.application.command.MarkNotificationReadCommand;
import com.group3.vitamins.notification.application.result.MarkNotificationReadResult;
import com.group3.vitamins.notification.application.result.NotificationTargetResult;
import com.group3.vitamins.notification.domain.model.Notification;
import com.group3.vitamins.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationCommandService")
class NotificationCommandServiceTest {

    private static final String OWNER = "EMP003";
    private static final String OTHER = "EMP001";
    private static final Long NOTIFICATION_ID = 301L;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCommandService service;

    private LocalDateTime createdAt;

    @BeforeEach
    void setUp() {
        createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
    }

    private Notification unread() {
        return Notification.restore(NOTIFICATION_ID, OWNER, "APPROVAL_REQUESTED", "결재 요청", "내용",
                "APPROVAL", 55L, Map.of("revisionId", 56L), null, null, createdAt);
    }

    private Notification alreadyRead(LocalDateTime readAt) {
        return Notification.restore(NOTIFICATION_ID, OWNER, "APPROVAL_REQUESTED", "결재 요청", "내용",
                "APPROVAL", 55L, Map.of("revisionId", 56L), readAt, null, createdAt);
    }

    private Notification withoutTarget() {
        return Notification.restore(NOTIFICATION_ID, OWNER, "SYSTEM_NOTICE", "공지", "점검 안내",
                null, null, null, null, null, createdAt);
    }

    @Nested
    @DisplayName("알림 삭제 (ACT-001~003)")
    class Delete {

        @Test
        @DisplayName("본인 알림은 논리 삭제된다 — deleted_at 이 기록되고 하드 삭제하지 않는다")
        void deletesOwnNotification() {
            Notification notification = unread();
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

            service.deleteNotification(new DeleteNotificationCommand(NOTIFICATION_ID, OWNER));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("타인 알림 삭제는 403 — 저장까지 가지 않는다")
        void rejectsOtherUsersNotification() {
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.of(unread()));

            assertThatThrownBy(() -> service.deleteNotification(
                    new DeleteNotificationCommand(NOTIFICATION_ID, OTHER)))
                    .isInstanceOf(ForbiddenException.class);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("없거나 이미 삭제된 알림은 404 — 삭제분은 findActiveById 에서 걸러진다(ACT-003)")
        void rejectsMissingOrAlreadyDeleted() {
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteNotification(
                    new DeleteNotificationCommand(NOTIFICATION_ID, OWNER)))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("개별 읽음 처리 (ACT-006)")
    class MarkRead {

        @Test
        @DisplayName("안 읽은 알림은 read_at 이 기록된다")
        void marksUnread() {
            Notification notification = unread();
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MarkNotificationReadResult result =
                    service.markRead(new MarkNotificationReadCommand(NOTIFICATION_ID, OWNER));

            assertThat(result.notificationId()).isEqualTo(NOTIFICATION_ID);
            assertThat(result.readAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 읽은 알림을 다시 호출해도 최초 읽음 시각을 덮어쓰지 않는다(멱등)")
        void isIdempotent() {
            LocalDateTime firstReadAt = LocalDateTime.of(2026, 8, 2, 10, 30);
            when(notificationRepository.findActiveById(NOTIFICATION_ID))
                    .thenReturn(Optional.of(alreadyRead(firstReadAt)));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MarkNotificationReadResult result =
                    service.markRead(new MarkNotificationReadCommand(NOTIFICATION_ID, OWNER));

            assertThat(result.readAt()).isEqualTo(firstReadAt);
        }

        @Test
        @DisplayName("타인 알림 읽음 처리는 403")
        void rejectsOtherUsersNotification() {
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.of(unread()));

            assertThatThrownBy(() -> service.markRead(
                    new MarkNotificationReadCommand(NOTIFICATION_ID, OTHER)))
                    .isInstanceOf(ForbiddenException.class);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("없거나 이미 삭제된 알림은 404")
        void rejectsMissingOrAlreadyDeleted() {
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markRead(
                    new MarkNotificationReadCommand(NOTIFICATION_ID, OWNER)))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("이동 대상 조회 (VIW-006~010 · ACT-004)")
    class GetTarget {

        @Test
        @DisplayName("저장된 이동 대상을 그대로 반환한다 — 알림 도메인이 값을 해석하지 않는다")
        void returnsStoredTargetAsIs() {
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.of(unread()));

            NotificationTargetResult result =
                    service.getTarget(new GetNotificationTargetCommand(NOTIFICATION_ID, OWNER));

            assertThat(result.type()).isEqualTo("APPROVAL");
            assertThat(result.targetId()).isEqualTo(55L);
            assertThat(result.extra()).containsEntry("revisionId", 56L);
        }

        @Test
        @DisplayName("이동 대상이 없는 알림은 type=NONE (에러 아님)")
        void returnsNoneWhenTargetAbsent() {
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.of(withoutTarget()));

            NotificationTargetResult result =
                    service.getTarget(new GetNotificationTargetCommand(NOTIFICATION_ID, OWNER));

            assertThat(result.type()).isEqualTo("NONE");
            assertThat(result.targetId()).isNull();
            assertThat(result.extra()).isNull();
        }

        @Test
        @DisplayName("조회 성공 시 자동으로 읽음 처리된다(ACT-004)")
        void marksReadOnLookup() {
            Notification notification = unread();
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

            service.getTarget(new GetNotificationTargetCommand(NOTIFICATION_ID, OWNER));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("타인 알림 조회는 403")
        void rejectsOtherUsersNotification() {
            when(notificationRepository.findActiveById(NOTIFICATION_ID)).thenReturn(Optional.of(unread()));

            assertThatThrownBy(() -> service.getTarget(
                    new GetNotificationTargetCommand(NOTIFICATION_ID, OTHER)))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}

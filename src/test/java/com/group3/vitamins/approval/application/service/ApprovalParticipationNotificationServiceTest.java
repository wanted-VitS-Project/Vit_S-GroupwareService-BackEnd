package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.ApprovalParticipationNotificationPort;
import com.group3.vitamins.approval.application.port.ApprovalParticipationNotificationPort.Target;
import com.group3.vitamins.approval.application.port.ApprovalParticipationNotificationPort.Editor;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.employee.contract.EmployeeParticipationUnavailableEvent;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ApprovalParticipationNotificationService — 사원 참여 불가 알림")
class ApprovalParticipationNotificationServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final String UNAVAILABLE = "vitas-OUT";
    private static final String DRAFTER = "vitas-DRAFTER";
    private static final String ACTING = "vitas-ACTING";

    private ApprovalParticipationNotificationPort notificationPort;
    private EmployeeCatalogPort employeeCatalogPort;
    private DomainEventPublisher domainEventPublisher;
    private ApprovalParticipationNotificationService service;

    @BeforeEach
    void setUp() {
        notificationPort = Mockito.mock(ApprovalParticipationNotificationPort.class);
        employeeCatalogPort = Mockito.mock(EmployeeCatalogPort.class);
        domainEventPublisher = Mockito.mock(DomainEventPublisher.class);
        service = new ApprovalParticipationNotificationService(
                notificationPort, employeeCatalogPort, domainEventPublisher);
    }

    @Test
    @DisplayName("미처리 결재자가 이탈하면 유효 기안자에게 교체·제외 알림을 보낸다")
    void notifiesDrafterWhenPendingApproverLeaves() {
        Target target = target(DRAFTER, null);
        when(notificationPort.findPendingApproverTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of(target));
        when(notificationPort.findDrafterTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of());
        when(employeeCatalogPort.findEmployee(DRAFTER)).thenReturn(Optional.of(active(DRAFTER)));

        service.notifyParticipationUnavailable(event());

        NotificationRequestedEvent notification = capturedSingleNotification();
        assertThat(notification.recipientUserId()).isEqualTo(DRAFTER);
        assertThat(notification.notificationType()).isEqualTo("APPROVAL_APPROVER_UNAVAILABLE");
        assertThat(notification.targetType()).isEqualTo("APPROVAL");
        assertThat(notification.targetId()).isEqualTo(100L);
        assertThat(notification.targetContext()).containsEntry("revisionId", 200L);
    }

    @Test
    @DisplayName("현재 기안자가 이탈하면 활성 스텝 EDITOR 전원에게 대행 알림을 중복 없이 보낸다")
    void notifiesActiveStepEditorsWhenDrafterLeaves() {
        Target target = target(UNAVAILABLE, null);
        when(notificationPort.findPendingApproverTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of());
        when(notificationPort.findDrafterTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of(target));
        when(notificationPort.findActiveStepEditors(Set.of(300L), COMPANY_ID))
                .thenReturn(List.of(
                        new Editor(300L, "vitas-E1"),
                        new Editor(300L, "vitas-E2"),
                        new Editor(300L, "vitas-E1")));

        service.notifyParticipationUnavailable(event());

        ArgumentCaptor<com.group3.vitamins.global.domain.event.DomainEvent> captor =
                ArgumentCaptor.forClass(com.group3.vitamins.global.domain.event.DomainEvent.class);
        verify(domainEventPublisher, Mockito.times(2)).publish(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(value -> assertThat(value)
                        .isInstanceOfSatisfying(NotificationRequestedEvent.class, notification -> {
                            assertThat(notification.notificationType())
                                    .isEqualTo("APPROVAL_DRAFTER_UNAVAILABLE");
                            assertThat(notification.targetId()).isEqualTo(100L);
                        }));
        assertThat(captor.getAllValues().stream()
                .map(NotificationRequestedEvent.class::cast)
                .map(NotificationRequestedEvent::recipientUserId))
                .containsExactly("vitas-E1", "vitas-E2");
    }

    @Test
    @DisplayName("원 기안자가 이탈해도 유효한 대행 기안자가 이미 있으면 EDITOR에게 다시 알리지 않는다")
    void skipsEditorsWhenActingDrafterAlreadyOwnsApproval() {
        Target target = target(UNAVAILABLE, ACTING);
        when(notificationPort.findPendingApproverTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of());
        when(notificationPort.findDrafterTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of(target));

        service.notifyParticipationUnavailable(event());

        verify(notificationPort).findActiveStepEditors(Set.of(), COMPANY_ID);
        verify(domainEventPublisher, never()).publish(Mockito.any());
    }

    @Test
    @DisplayName("현재 기안자가 참여 불가이면 결재자 이탈 알림 수신자에서 제외한다")
    void skipsUnavailableCurrentDrafter() {
        Target target = target(DRAFTER, null);
        when(notificationPort.findPendingApproverTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of(target));
        when(notificationPort.findDrafterTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of());
        when(employeeCatalogPort.findEmployee(DRAFTER)).thenReturn(Optional.of(inactive(DRAFTER, "MEMBER")));

        service.notifyParticipationUnavailable(event());

        verify(domainEventPublisher, never()).publish(Mockito.any());
    }

    @Test
    @DisplayName("현재 기안자가 ADMIN이면 결재자 이탈 알림 수신자에서 제외한다")
    void skipsAdminCurrentDrafter() {
        Target target = target(DRAFTER, null);
        when(notificationPort.findPendingApproverTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of(target));
        when(notificationPort.findDrafterTargets(UNAVAILABLE, COMPANY_ID)).thenReturn(List.of());
        when(employeeCatalogPort.findEmployee(DRAFTER)).thenReturn(Optional.of(active(DRAFTER, "ADMIN")));

        service.notifyParticipationUnavailable(event());

        verify(domainEventPublisher, never()).publish(Mockito.any());
    }

    private NotificationRequestedEvent capturedSingleNotification() {
        ArgumentCaptor<com.group3.vitamins.global.domain.event.DomainEvent> captor =
                ArgumentCaptor.forClass(com.group3.vitamins.global.domain.event.DomainEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        return (NotificationRequestedEvent) captor.getValue();
    }

    private EmployeeParticipationUnavailableEvent event() {
        return new EmployeeParticipationUnavailableEvent(UNAVAILABLE, COMPANY_ID);
    }

    private Target target(String drafterId, String actingDrafterId) {
        return new Target(100L, 200L, 300L, "기술 제안서", drafterId, actingDrafterId);
    }

    private EmployeeSummary active(String userId) {
        return active(userId, "MEMBER");
    }

    private EmployeeSummary active(String userId, String role) {
        return new EmployeeSummary(userId, "사용자", null, null, role, COMPANY_ID,
                "ACTIVE", null, null);
    }

    private EmployeeSummary inactive(String userId, String role) {
        return new EmployeeSummary(userId, "사용자", null, null, role, COMPANY_ID,
                "INACTIVE", null, null);
    }
}

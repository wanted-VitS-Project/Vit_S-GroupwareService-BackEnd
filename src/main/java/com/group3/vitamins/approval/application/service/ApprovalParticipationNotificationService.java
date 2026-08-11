package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.ApprovalParticipationNotificationPort;
import com.group3.vitamins.approval.application.port.ApprovalParticipationNotificationPort.Editor;
import com.group3.vitamins.approval.application.port.ApprovalParticipationNotificationPort.Target;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.employee.contract.EmployeeParticipationUnavailableEvent;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 사원의 업무 참여 불가 전환을 결재 후속 조치 알림으로 변환한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalParticipationNotificationService {

    static final String APPROVER_UNAVAILABLE_TYPE = "APPROVAL_APPROVER_UNAVAILABLE";
    static final String DRAFTER_UNAVAILABLE_TYPE = "APPROVAL_DRAFTER_UNAVAILABLE";
    private static final String TARGET_TYPE = "APPROVAL";

    private final ApprovalParticipationNotificationPort notificationPort;
    private final EmployeeCatalogPort employeeCatalogPort;
    private final DomainEventPublisher domainEventPublisher;

    public void notifyParticipationUnavailable(EmployeeParticipationUnavailableEvent event) {
        notifyDraftersOfUnavailableApprover(event);
        notifyEditorsOfUnavailableDrafter(event);
    }

    private void notifyDraftersOfUnavailableApprover(EmployeeParticipationUnavailableEvent event) {
        for (Target target : notificationPort.findPendingApproverTargets(event.userId(), event.companyId())) {
            String recipient = availableCurrentDrafter(target);
            if (recipient == null) {
                continue;
            }
            publish(recipient, APPROVER_UNAVAILABLE_TYPE, "결재자 재지정 필요",
                    displayTitle(target) + " 결재의 결재자가 참여할 수 없어 교체 또는 제외가 필요합니다.", target);
        }
    }

    private void notifyEditorsOfUnavailableDrafter(EmployeeParticipationUnavailableEvent event) {
        List<Target> targets = notificationPort.findDrafterTargets(event.userId(), event.companyId()).stream()
                // 원 기안자가 이탈했어도 이미 유효한 대행 기안자가 있으면 새 담당자를 찾을 필요가 없다.
                .filter(target -> event.userId().equals(currentDrafterId(target)))
                .toList();
        Set<Long> blockIds = targets.stream()
                .map(Target::blockId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Set<String>> editorsByBlock = notificationPort
                .findActiveStepEditors(blockIds, event.companyId()).stream()
                .collect(Collectors.groupingBy(
                        Editor::blockId,
                        Collectors.mapping(Editor::userId,
                                Collectors.toCollection(LinkedHashSet::new))));

        for (Target target : targets) {
            Set<String> recipients = editorsByBlock.getOrDefault(target.blockId(), Set.of());
            recipients.forEach(recipient -> publish(
                    recipient, DRAFTER_UNAVAILABLE_TYPE, "대행 기안자 필요",
                    displayTitle(target) + " 결재의 기안자가 참여할 수 없어 대행 처리가 필요합니다.", target));
        }
    }

    private String availableCurrentDrafter(Target target) {
        String current = currentDrafterId(target);
        if (current == null) {
            return null;
        }
        return employeeCatalogPort.findEmployee(current)
                .filter(employee -> !employee.participationUnavailable())
                .filter(employee -> !"ADMIN".equals(employee.role()))
                .map(EmployeeSummary::userId)
                .orElse(null);
    }

    private String currentDrafterId(Target target) {
        return target.actingDrafterId() == null ? target.drafterId() : target.actingDrafterId();
    }

    private String displayTitle(Target target) {
        return target.title() == null || target.title().isBlank() ? "결재" : target.title();
    }

    private void publish(String recipient, String type, String title, String message, Target target) {
        domainEventPublisher.publish(NotificationRequestedEvent.of(
                recipient, type, title, message,
                TARGET_TYPE, target.approvalId(), Map.of("revisionId", target.revisionId())));
        log.info("결재 참여 불가 알림 발행 - approvalId={}, type={}, recipient={}",
                target.approvalId(), type, recipient);
    }
}

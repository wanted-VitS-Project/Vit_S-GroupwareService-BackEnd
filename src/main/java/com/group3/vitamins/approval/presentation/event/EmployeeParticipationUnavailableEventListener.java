package com.group3.vitamins.approval.presentation.event;

import com.group3.vitamins.approval.application.service.ApprovalParticipationNotificationService;
import com.group3.vitamins.employee.contract.EmployeeParticipationUnavailableEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 사원 참여 불가 전환이 커밋된 뒤 새 트랜잭션에서 결재 알림 요청으로 연결한다. */
@Component
@RequiredArgsConstructor
public class EmployeeParticipationUnavailableEventListener {

    private final ApprovalParticipationNotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EmployeeParticipationUnavailableEvent event) {
        notificationService.notifyParticipationUnavailable(event);
    }
}

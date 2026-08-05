package com.group3.vitamins.activitylog.presentation.event;

import com.group3.vitamins.activitylog.application.service.ActivityLogRecordService;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {

    private final ActivityLogRecordService activityLogRecordService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(ActivityOccurredEvent event) {
        activityLogRecordService.write(event);
    }
}

package com.group3.vitamins.activitylog.application;

import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {

    private final ActivityLogWriter activityLogWriter;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(ActivityOccurredEvent event) {
        activityLogWriter.write(event);
    }
}

package com.group3.vitamins.activitylog.application;

import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogWriter {

    private final ActivityLogRecorder activityLogRecorder;

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(ActivityOccurredEvent event) {
        activityLogRecorder.record(event);
    }
}

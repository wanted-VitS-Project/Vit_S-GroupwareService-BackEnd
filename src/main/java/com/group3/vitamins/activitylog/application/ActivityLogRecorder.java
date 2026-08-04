package com.group3.vitamins.activitylog.application;

import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;

public interface ActivityLogRecorder {

    void record(ActivityOccurredEvent event);
}

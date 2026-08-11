package com.group3.vitamins.activitylog.application.port;

import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;

public interface ActivityLogRecordPort {

    void record(ActivityOccurredEvent event, Long companyId);
}

package com.group3.vitamins.activitylog.application.service;

import com.group3.vitamins.activitylog.application.port.ActivityLogRecordPort;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogRecordService {

    private final ActivityLogRecordPort activityLogRecordPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(ActivityOccurredEvent event) {
        activityLogRecordPort.record(event, currentCompanyIdProvider.currentCompanyId());
    }
}

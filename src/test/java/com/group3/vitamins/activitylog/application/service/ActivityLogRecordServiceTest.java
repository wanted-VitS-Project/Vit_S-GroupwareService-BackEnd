package com.group3.vitamins.activitylog.application.service;

import com.group3.vitamins.activitylog.application.port.ActivityLogRecordPort;
import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Activity Log Record Service")
class ActivityLogRecordServiceTest {

    @Test
    @DisplayName("수신한 이벤트와 현재 회사를 저장 포트에 위임한다")
    void delegatesEventToRecorder() {
        ActivityLogRecordPort activityLogRecordPort = mock(ActivityLogRecordPort.class);
        CurrentCompanyIdProvider currentCompanyIdProvider = mock(CurrentCompanyIdProvider.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(7L);
        ActivityLogRecordService service = new ActivityLogRecordService(
                activityLogRecordPort, currentCompanyIdProvider);
        ActivityOccurredEvent event = ActivityOccurredEvent.of(
                ActivityLogAction.MODIFY,
                30L,
                30L,
                "제안서 작성",
                "EMP001",
                List.of(
                        new ActivityFieldChange("title", "제안서", "제안서 작성"),
                        new ActivityFieldChange("rowIndex", "1", "2")
                )
        );

        service.write(event);

        ArgumentCaptor<ActivityOccurredEvent> eventCaptor = ArgumentCaptor.forClass(ActivityOccurredEvent.class);
        ArgumentCaptor<Long> companyIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(activityLogRecordPort).record(eventCaptor.capture(), companyIdCaptor.capture());
        assertThat(eventCaptor.getValue()).isSameAs(event);
        assertThat(companyIdCaptor.getValue()).isEqualTo(7L);
    }
}

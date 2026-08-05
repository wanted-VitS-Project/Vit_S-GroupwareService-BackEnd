package com.group3.vitamins.activitylog.presentation.event;

import com.group3.vitamins.activitylog.application.service.ActivityLogRecordService;
import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Activity Log 이벤트 수집")
class ActivityLogEventListenerTest {

    @Test
    @DisplayName("이벤트를 수신하면 ActivityLogRecordService에 전달한다")
    void delegatesEventToRecordService() {
        ActivityLogRecordService activityLogRecordService = mock(ActivityLogRecordService.class);
        ActivityLogEventListener listener = new ActivityLogEventListener(activityLogRecordService);
        ActivityOccurredEvent event = ActivityOccurredEvent.of(
                ActivityLogAction.DELETE,
                30L,
                null,
                null,
                "EMP001",
                List.of(new ActivityFieldChange(null, "제안서", null))
        );

        listener.handle(event);

        ArgumentCaptor<ActivityOccurredEvent> captor = ArgumentCaptor.forClass(ActivityOccurredEvent.class);
        verify(activityLogRecordService).write(captor.capture());
        assertThat(captor.getValue()).isSameAs(event);
    }
}

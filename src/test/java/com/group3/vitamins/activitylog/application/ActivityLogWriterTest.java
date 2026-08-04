package com.group3.vitamins.activitylog.application;

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

@DisplayName("Activity Log Writer")
class ActivityLogWriterTest {

    @Test
    @DisplayName("수신한 이벤트를 저장 포트에 위임한다")
    void delegatesEventToRecorder() {
        ActivityLogRecorder activityLogRecorder = mock(ActivityLogRecorder.class);
        ActivityLogWriter writer = new ActivityLogWriter(activityLogRecorder);
        ActivityOccurredEvent event = ActivityOccurredEvent.of(
                ActivityLogAction.MODIFY,
                30L,
                30L,
                "EMP001",
                List.of(
                        new ActivityFieldChange("title", "제안서", "제안서 작성"),
                        new ActivityFieldChange("rowIndex", "1", "2")
                )
        );

        writer.write(event);

        ArgumentCaptor<ActivityOccurredEvent> captor = ArgumentCaptor.forClass(ActivityOccurredEvent.class);
        verify(activityLogRecorder).record(captor.capture());
        assertThat(captor.getValue()).isSameAs(event);
    }
}

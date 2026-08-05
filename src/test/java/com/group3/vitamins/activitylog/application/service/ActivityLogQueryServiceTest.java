package com.group3.vitamins.activitylog.application.service;

import com.group3.vitamins.activitylog.application.port.ActivityLogQueryPort;
import com.group3.vitamins.activitylog.application.query.ActivityLogListQuery;
import com.group3.vitamins.activitylog.domain.exception.ActivityLogErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("Activity Log Query Service")
class ActivityLogQueryServiceTest {

    private final ActivityLogQueryPort activityLogQueryPort = mock(ActivityLogQueryPort.class);
    private final ActivityLogQueryService service = new ActivityLogQueryService(activityLogQueryPort);

    @Test
    @DisplayName("조회 size가 최대값을 초과하면 예외를 던진다")
    void getActivityLogs_sizeOverMax() {
        ActivityLogListQuery query = new ActivityLogListQuery(
                1L,
                null,
                null,
                101,
                "EMP001",
                "USER"
        );

        assertThatThrownBy(() -> service.getActivityLogs(query))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ActivityLogErrorCode.ACTIVITY_LOG_SIZE_INVALID));
    }
}

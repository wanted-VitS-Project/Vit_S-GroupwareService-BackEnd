package com.group3.vitamins.activitylog.application.service;

import com.group3.vitamins.activitylog.application.port.ActivityLogQueryPort;
import com.group3.vitamins.activitylog.application.query.ActivityLogListQuery;
import com.group3.vitamins.activitylog.application.result.StepAccessResult;
import com.group3.vitamins.activitylog.domain.exception.ActivityLogErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Activity Log Query Service")
class ActivityLogQueryServiceTest {

    private final ActivityLogQueryPort activityLogQueryPort = mock(ActivityLogQueryPort.class);
    private final CurrentCompanyIdProvider currentCompanyIdProvider = mock(CurrentCompanyIdProvider.class);
    private final ActivityLogQueryService service = new ActivityLogQueryService(
            activityLogQueryPort, currentCompanyIdProvider);

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

    @Test
    @DisplayName("현재 회사 ID를 로그 목록 조회 포트에 전달한다")
    void getActivityLogs_passesCurrentCompanyId() {
        ActivityLogListQuery query = new ActivityLogListQuery(
                1L,
                null,
                null,
                null,
                "EMP001",
                "USER"
        );
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(7L);
        when(activityLogQueryPort.findStepAccess(1L, "EMP001", 7L))
                .thenReturn(Optional.of(new StepAccessResult(1L, 10L, "VIEWER")));
        when(activityLogQueryPort.findActivityLogs(1L, null, null, 21, 7L))
                .thenReturn(List.of());

        service.getActivityLogs(query);

        verify(activityLogQueryPort).findActivityLogs(1L, null, null, 21, 7L);
    }
}

package com.group3.vitamins.activitylog.presentation.api.response;

import com.group3.vitamins.activitylog.application.result.ActivityLogResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActivityLogActorResponse")
class ActivityLogActorResponseTest {

    @Test
    @DisplayName("퇴사한 수행자의 퇴사일을 응답에 포함한다")
    void from_includesActorResignedAt() {
        ActivityLogActorResponse response = ActivityLogActorResponse.from(
                new ActivityLogResult.Actor("EMP003", "이영희", LocalDate.of(2026, 8, 1)));

        assertThat(response).isEqualTo(new ActivityLogActorResponse(
                "EMP003", "이영희", LocalDate.of(2026, 8, 1)));
    }
}

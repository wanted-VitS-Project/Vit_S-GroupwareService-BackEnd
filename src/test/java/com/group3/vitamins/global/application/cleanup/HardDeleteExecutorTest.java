package com.group3.vitamins.global.application.cleanup;

import com.group3.vitamins.global.application.cleanup.port.HardDeleteTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HardDeleteExecutor")
class HardDeleteExecutorTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-08T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("보존 기간만큼 뺀 기준 시각으로 대상을 실행한다")
        void executesTargetWithThresholdBeforeRetention() {
            HardDeleteTarget target = mock(HardDeleteTarget.class);
            when(target.targetName()).thenReturn("example");
            when(target.retention()).thenReturn(Period.ofDays(30));
            when(target.hardDeleteBefore(any())).thenReturn(5);

            HardDeleteExecutor executor = new HardDeleteExecutor(List.of(target), FIXED_CLOCK);

            int deletedCount = executor.execute(target);

            assertThat(deletedCount).isEqualTo(5);
            verify(target).hardDeleteBefore(LocalDateTime.now(FIXED_CLOCK).minus(Period.ofDays(30)));
        }

        @Test
        @DisplayName("대상이 null이면 거부한다")
        void rejectsNullTarget() {
            HardDeleteExecutor executor = new HardDeleteExecutor(List.of(), FIXED_CLOCK);

            assertThatThrownBy(() -> executor.execute(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("executeAll")
    class ExecuteAll {

        private HardDeleteTarget succeeding;
        private HardDeleteTarget failing;

        @BeforeEach
        void setUp() {
            succeeding = mock(HardDeleteTarget.class);
            when(succeeding.targetName()).thenReturn("succeeding");
            when(succeeding.retention()).thenReturn(Period.ofDays(1));
            when(succeeding.hardDeleteBefore(any())).thenReturn(3);

            failing = mock(HardDeleteTarget.class);
            when(failing.targetName()).thenReturn("failing");
            when(failing.retention()).thenReturn(Period.ofDays(1));
            when(failing.hardDeleteBefore(any())).thenThrow(new RuntimeException("boom"));
        }

        @Test
        @DisplayName("등록된 대상이 없으면 0을 반환하고 아무 것도 실행하지 않는다")
        void returnsZeroWhenNoTargetsRegistered() {
            HardDeleteExecutor executor = new HardDeleteExecutor(List.of(), FIXED_CLOCK);

            int totalDeletedCount = executor.executeAll();

            assertThat(totalDeletedCount).isZero();
        }

        @Test
        @DisplayName("모든 대상의 삭제 건수를 합산한다")
        void sumsDeletedCountAcrossTargets() {
            HardDeleteTarget other = mock(HardDeleteTarget.class);
            when(other.targetName()).thenReturn("other");
            when(other.retention()).thenReturn(Period.ofDays(1));
            when(other.hardDeleteBefore(any())).thenReturn(2);

            HardDeleteExecutor executor = new HardDeleteExecutor(List.of(succeeding, other), FIXED_CLOCK);

            int totalDeletedCount = executor.executeAll();

            assertThat(totalDeletedCount).isEqualTo(5);
        }

        @Test
        @DisplayName("한 대상이 실패해도 나머지 대상은 계속 실행한다")
        void isolatesFailureOfSingleTarget() {
            HardDeleteExecutor executor = new HardDeleteExecutor(List.of(failing, succeeding), FIXED_CLOCK);

            int totalDeletedCount = executor.executeAll();

            assertThat(totalDeletedCount).isEqualTo(3);
            verify(failing, times(1)).hardDeleteBefore(any());
            verify(succeeding, times(1)).hardDeleteBefore(any());
        }
    }
}

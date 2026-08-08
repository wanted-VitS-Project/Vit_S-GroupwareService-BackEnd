package com.group3.vitamins.global.application.cleanup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DefaultHardDeleteTarget")
class DefaultHardDeleteTargetTest {

    @Nested
    @DisplayName("생성")
    class Construction {

        @Test
        @DisplayName("이름이 비어 있으면 거부한다")
        void rejectsBlankTargetName() {
            assertThatThrownBy(() -> new DefaultHardDeleteTarget(" ", Period.ofDays(1), threshold -> 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("보존 기간이 0 이하면 거부한다")
        void rejectsNonPositiveRetention() {
            assertThatThrownBy(() -> new DefaultHardDeleteTarget("example", Period.ZERO, threshold -> 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("hardDeleteBefore")
    class HardDeleteBefore {

        @Test
        @DisplayName("주입된 삭제 동작에 기준 시각을 그대로 위임한다")
        void delegatesToOperation() {
            var operation = mock(com.group3.vitamins.global.application.cleanup.port.HardDeleteOperation.class);
            when(operation.deleteBefore(any(LocalDateTime.class))).thenReturn(7);
            var target = new DefaultHardDeleteTarget("example", Period.ofDays(1), operation);

            LocalDateTime threshold = LocalDateTime.of(2026, 8, 1, 3, 0);
            int deletedCount = target.hardDeleteBefore(threshold);

            assertThat(deletedCount).isEqualTo(7);
            verify(operation).deleteBefore(threshold);
        }

        @Test
        @DisplayName("기준 시각이 null이면 거부한다")
        void rejectsNullThreshold() {
            var target = new DefaultHardDeleteTarget("example", Period.ofDays(1), threshold -> 0);

            assertThatThrownBy(() -> target.hardDeleteBefore(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}

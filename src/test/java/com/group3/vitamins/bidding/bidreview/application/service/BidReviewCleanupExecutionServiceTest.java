package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCleanupStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BidReviewCleanupExecutionService 정리 Outbox 점유·실행")
class BidReviewCleanupExecutionServiceTest {

    @Test
    @DisplayName("Outbox를 한 건씩 새로 점유하여 배치 도중 잠금 만료를 방지한다")
    void cleansUpOneAtATime() {
        BidReviewCleanupStorePort store = mock(BidReviewCleanupStorePort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T03:00:00Z"), ZoneOffset.UTC);
        var service = new BidReviewCleanupExecutionService(store, clock);

        when(store.claimNext(eq("server-1"), any(), any()))
                .thenReturn(Optional.of(1L), Optional.of(2L), Optional.empty());

        int cleanedCount = service.cleanupBatch("server-1", 10);

        assertThat(cleanedCount).isEqualTo(2);
        verify(store, times(3)).claimNext(eq("server-1"), any(), any());
        verify(store).execute(eq(1L), eq("server-1"), any());
        verify(store).execute(eq(2L), eq("server-1"), any());
        verify(store, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    @DisplayName("실행이 실패하면 실패로 기록하고 다음 배치 항목은 계속 시도한다")
    void continuesAfterFailure() {
        BidReviewCleanupStorePort store = mock(BidReviewCleanupStorePort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T03:00:00Z"), ZoneOffset.UTC);
        var service = new BidReviewCleanupExecutionService(store, clock);

        when(store.claimNext(eq("server-1"), any(), any()))
                .thenReturn(Optional.of(1L), Optional.of(2L), Optional.empty());
        doThrow(new IllegalStateException("s3 down")).when(store).execute(eq(1L), any(), any());

        int cleanedCount = service.cleanupBatch("server-1", 10);

        assertThat(cleanedCount).isEqualTo(1);
        verify(store).markFailed(eq(1L), eq("server-1"), any(), any());
        verify(store).execute(eq(2L), eq("server-1"), any());
    }

    @Test
    @DisplayName("점유할 게 없으면 즉시 종료한다")
    void stopsWhenNothingToClaim() {
        BidReviewCleanupStorePort store = mock(BidReviewCleanupStorePort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T03:00:00Z"), ZoneOffset.UTC);
        var service = new BidReviewCleanupExecutionService(store, clock);

        when(store.claimNext(eq("server-1"), any(), any())).thenReturn(Optional.empty());

        int cleanedCount = service.cleanupBatch("server-1", 10);

        assertThat(cleanedCount).isZero();
        verify(store, never()).execute(any(), any(), any());
    }
}

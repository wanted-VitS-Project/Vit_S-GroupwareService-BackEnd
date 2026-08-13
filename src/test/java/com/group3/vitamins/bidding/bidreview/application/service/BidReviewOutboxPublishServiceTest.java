package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.model.ClaimedBidReviewOutbox;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewJobPublisherPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewOutboxStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BidReviewOutboxPublishService 발행 점유")
class BidReviewOutboxPublishServiceTest {

    @Test
    @DisplayName("Outbox를 한 건씩 새로 점유하여 배치 도중 잠금 만료를 방지한다")
    void claimsOneOutboxAtATime() {
        BidReviewOutboxStorePort store = mock(BidReviewOutboxStorePort.class);
        BidReviewJobPublisherPort publisher = mock(BidReviewJobPublisherPort.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-13T01:00:00Z"), ZoneOffset.UTC
        );
        var service = new BidReviewOutboxPublishService(store, publisher, clock, 300);
        var first = outbox(1L, 1);
        var second = outbox(2L, 1);
        when(store.claimPublishable(eq("server-1"), eq(1), any(), any()))
                .thenReturn(List.of(first), List.of(second), List.of());

        int publishedCount = service.publishBatch("server-1", 10);

        assertThat(publishedCount).isEqualTo(2);
        verify(store, times(3)).claimPublishable(eq("server-1"), eq(1), any(), any());
        verify(publisher).publish(first);
        verify(publisher).publish(second);
        verify(store).markPublished(eq(1L), eq("server-1"), any());
        verify(store).markPublished(eq(2L), eq("server-1"), any());
    }

    @Test
    @DisplayName("발행 실패 시 재시도 지연시각을 계산해 markPublishFailed를 호출한다")
    void marksFailedOnPublishError() {
        BidReviewOutboxStorePort store = mock(BidReviewOutboxStorePort.class);
        BidReviewJobPublisherPort publisher = mock(BidReviewJobPublisherPort.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-13T01:00:00Z"), ZoneOffset.UTC
        );
        var service = new BidReviewOutboxPublishService(store, publisher, clock, 300);
        var outbox = outbox(1L, 1);
        when(store.claimPublishable(eq("server-1"), eq(1), any(), any()))
                .thenReturn(List.of(outbox), List.of());
        doThrow(new IllegalStateException("redis down")).when(publisher).publish(outbox);

        int publishedCount = service.publishBatch("server-1", 10);

        assertThat(publishedCount).isZero();
        verify(store).markPublishFailed(eq(1L), eq("server-1"), eq("REDIS_PUBLISH_FAILED"), any(), any());
        verify(store, never()).markPublished(any(), any(), any());
    }

    private ClaimedBidReviewOutbox outbox(Long outboxId, int attemptCount) {
        return new ClaimedBidReviewOutbox(
                outboxId,
                "event-" + outboxId,
                "BID_REVIEW_REQUESTED",
                100L + outboxId,
                10L,
                "00000000-0000-0000-0000-00000000000" + outboxId,
                0,
                attemptCount
        );
    }
}
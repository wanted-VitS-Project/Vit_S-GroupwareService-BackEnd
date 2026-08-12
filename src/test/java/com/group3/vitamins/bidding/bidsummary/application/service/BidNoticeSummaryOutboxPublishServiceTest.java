package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.model.ClaimedBidNoticeSummaryOutbox;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryJobPublisherPort;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryOutboxStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryOutboxPublishService 발행 점유")
class BidNoticeSummaryOutboxPublishServiceTest {

    @Test
    @DisplayName("Outbox를 한 건씩 새로 점유하여 배치 도중 잠금 만료를 방지한다")
    void claimsOneOutboxAtATime() {
        BidNoticeSummaryOutboxStorePort store =
                mock(BidNoticeSummaryOutboxStorePort.class);
        BidNoticeSummaryJobPublisherPort publisher =
                mock(BidNoticeSummaryJobPublisherPort.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T01:00:00Z"), ZoneOffset.UTC
        );
        var service = new BidNoticeSummaryOutboxPublishService(
                store, publisher, clock, 300
        );
        var first = outbox(1L, 1);
        var second = outbox(2L, 1);
        when(store.claimPublishable(eq("server-1"), eq(1), any(), any()))
                .thenReturn(List.of(first), List.of(second), List.of());

        int publishedCount = service.publishBatch("server-1", 10);

        assertThat(publishedCount).isEqualTo(2);
        verify(store, times(3)).claimPublishable(
                eq("server-1"), eq(1), any(), any()
        );
        verify(publisher).publish(first);
        verify(publisher).publish(second);
        verify(store).markPublished(eq(1L), eq("server-1"), any());
        verify(store).markPublished(eq(2L), eq("server-1"), any());
    }

    private ClaimedBidNoticeSummaryOutbox outbox(Long outboxId, int attemptCount) {
        return new ClaimedBidNoticeSummaryOutbox(
                outboxId,
                "event-" + outboxId,
                "BID_NOTICE_SUMMARY_REQUESTED",
                100L + outboxId,
                10L,
                "00000000-0000-0000-0000-00000000000" + outboxId,
                0,
                attemptCount
        );
    }
}

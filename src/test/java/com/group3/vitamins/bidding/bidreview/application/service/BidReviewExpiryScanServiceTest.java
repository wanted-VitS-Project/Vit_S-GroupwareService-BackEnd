package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewExpiryScanPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BidReviewExpiryScanService 만료 후보 점유")
class BidReviewExpiryScanServiceTest {

    @Test
    @DisplayName("후보 목록을 조회해 하나씩 점유를 시도하고 실제 점유한 건수만 센다")
    void claimsOnlyEligibleCandidates() {
        BidReviewExpiryScanPort scanPort = mock(BidReviewExpiryScanPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T03:00:00Z"), ZoneOffset.UTC);
        var service = new BidReviewExpiryScanService(scanPort, clock);

        when(scanPort.findExpiredCandidateIds(any(), eq(10)))
                .thenReturn(List.of(1L, 2L, 3L));
        when(scanPort.claimAndRequestCleanup(eq(1L), any())).thenReturn(true);
        when(scanPort.claimAndRequestCleanup(eq(2L), any())).thenReturn(false);
        when(scanPort.claimAndRequestCleanup(eq(3L), any())).thenReturn(true);

        int claimedCount = service.scanBatch(10);

        assertThat(claimedCount).isEqualTo(2);
        verify(scanPort).claimAndRequestCleanup(eq(1L), any());
        verify(scanPort).claimAndRequestCleanup(eq(2L), any());
        verify(scanPort).claimAndRequestCleanup(eq(3L), any());
    }

    @Test
    @DisplayName("후보가 없으면 점유를 시도하지 않는다")
    void doesNothingWhenNoCandidates() {
        BidReviewExpiryScanPort scanPort = mock(BidReviewExpiryScanPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T03:00:00Z"), ZoneOffset.UTC);
        var service = new BidReviewExpiryScanService(scanPort, clock);

        when(scanPort.findExpiredCandidateIds(any(), eq(10))).thenReturn(List.of());

        int claimedCount = service.scanBatch(10);

        assertThat(claimedCount).isZero();
        verify(scanPort, never()).claimAndRequestCleanup(any(), any());
    }
}

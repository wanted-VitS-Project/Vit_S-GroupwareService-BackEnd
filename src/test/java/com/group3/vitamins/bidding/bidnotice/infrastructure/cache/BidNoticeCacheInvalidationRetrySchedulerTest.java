package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeListCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class BidNoticeCacheInvalidationRetrySchedulerTest {

    private BidNoticeListCachePort cachePort;
    private BidNoticeCacheInvalidationRetryQueue retryQueue;
    private BidNoticeCacheInvalidationRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        cachePort = mock(BidNoticeListCachePort.class);
        retryQueue = new BidNoticeCacheInvalidationRetryQueue();
        scheduler = new BidNoticeCacheInvalidationRetryScheduler(cachePort, retryQueue);
        ReflectionTestUtils.setField(scheduler, "batchSize", 100);
    }

    @Test
    void removesCompanyAfterSuccessfulRetry() {
        retryQueue.enqueue(10L);
        when(cachePort.invalidate(10L)).thenReturn(true);

        scheduler.retryPendingInvalidations();

        verify(cachePort).invalidate(10L);
        assertThat(retryQueue.size()).isZero();
    }

    @Test
    void keepsCompanyAfterFailedRetry() {
        retryQueue.enqueue(10L);
        when(cachePort.invalidate(10L)).thenReturn(false);

        scheduler.retryPendingInvalidations();

        assertThat(retryQueue.size()).isEqualTo(1);
    }

    @Test
    void keepsCompanyWhenRetryThrowsUnexpectedException() {
        retryQueue.enqueue(10L);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(cachePort).invalidate(10L);

        scheduler.retryPendingInvalidations();

        assertThat(retryQueue.size()).isEqualTo(1);
    }
}

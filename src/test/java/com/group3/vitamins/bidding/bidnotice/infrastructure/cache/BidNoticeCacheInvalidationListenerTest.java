package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeListCachePort;
import com.group3.vitamins.bidding.bidnotice.domain.event.BidNoticeListChangedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class BidNoticeCacheInvalidationListenerTest {

    @Test
    void invalidatesOnlyChangedCompanyCache() {
        BidNoticeListCachePort cachePort = mock(BidNoticeListCachePort.class);
        BidNoticeCacheInvalidationRetryQueue retryQueue =
                new BidNoticeCacheInvalidationRetryQueue();
        when(cachePort.invalidate(10L)).thenReturn(true);
        BidNoticeCacheInvalidationListener listener =
                new BidNoticeCacheInvalidationListener(cachePort, retryQueue);

        listener.handle(new BidNoticeListChangedEvent(10L));

        verify(cachePort).invalidate(10L);
        assertThat(retryQueue.size()).isZero();
    }

    @Test
    void enqueuesCompanyWhenInvalidationFails() {
        BidNoticeListCachePort cachePort = mock(BidNoticeListCachePort.class);
        BidNoticeCacheInvalidationRetryQueue retryQueue =
                new BidNoticeCacheInvalidationRetryQueue();
        when(cachePort.invalidate(10L)).thenReturn(false);
        BidNoticeCacheInvalidationListener listener =
                new BidNoticeCacheInvalidationListener(cachePort, retryQueue);

        listener.handle(new BidNoticeListChangedEvent(10L));

        assertThat(retryQueue.snapshot(10))
                .containsExactly(new BidNoticeCacheInvalidationRetryQueue.RetryEntry(10L, 1L));
    }
}

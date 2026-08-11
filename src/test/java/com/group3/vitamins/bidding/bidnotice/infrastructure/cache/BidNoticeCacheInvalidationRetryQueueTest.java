package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BidNoticeCacheInvalidationRetryQueueTest {

    @Test
    void deduplicatesCompanyAndAdvancesGeneration() {
        BidNoticeCacheInvalidationRetryQueue queue =
                new BidNoticeCacheInvalidationRetryQueue();

        queue.enqueue(10L);
        queue.enqueue(10L);

        assertThat(queue.snapshot(10))
                .containsExactly(new BidNoticeCacheInvalidationRetryQueue.RetryEntry(10L, 2L));
    }

    @Test
    void doesNotRemoveNewerInvalidationRegisteredDuringRetry() {
        BidNoticeCacheInvalidationRetryQueue queue =
                new BidNoticeCacheInvalidationRetryQueue();
        queue.enqueue(10L);
        BidNoticeCacheInvalidationRetryQueue.RetryEntry retrying =
                queue.snapshot(10).get(0);

        queue.enqueue(10L);
        queue.removeIfUnchanged(retrying);

        assertThat(queue.snapshot(10))
                .containsExactly(new BidNoticeCacheInvalidationRetryQueue.RetryEntry(10L, 2L));
    }
}

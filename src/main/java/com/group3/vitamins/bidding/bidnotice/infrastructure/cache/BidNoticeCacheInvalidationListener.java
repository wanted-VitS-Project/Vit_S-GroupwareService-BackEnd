package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeListCachePort;
import com.group3.vitamins.bidding.bidnotice.domain.event.BidNoticeListChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BidNoticeCacheInvalidationListener {

    private final BidNoticeListCachePort cachePort;
    private final BidNoticeCacheInvalidationRetryQueue retryQueue;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(BidNoticeListChangedEvent event) {
        if (!cachePort.invalidate(event.companyId())) {
            retryQueue.enqueue(event.companyId());
        }
    }
}

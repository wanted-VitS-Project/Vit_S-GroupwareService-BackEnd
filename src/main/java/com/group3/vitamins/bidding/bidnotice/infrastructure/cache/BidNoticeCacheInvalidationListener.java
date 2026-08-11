package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeListCachePort;
import com.group3.vitamins.bidding.bidnotice.domain.event.BidNoticeListChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BidNoticeCacheInvalidationListener {

    private final BidNoticeListCachePort cachePort;
    private final BidNoticeCacheInvalidationRetryQueue retryQueue;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(BidNoticeListChangedEvent event) {
        try {
            if (!cachePort.invalidate(event.companyId())) {
                retryQueue.enqueue(event.companyId());
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "Bidding notice cache invalidation listener failed. companyId={}",
                    event.companyId(),
                    exception
            );
            retryQueue.enqueue(event.companyId());
        }
    }
}

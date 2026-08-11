package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeListCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidNoticeCacheInvalidationRetryScheduler {

    private final BidNoticeListCachePort cachePort;
    private final BidNoticeCacheInvalidationRetryQueue retryQueue;

    @Value("${bidding.notice-cache.retry.batch-size:100}")
    private int batchSize;

    // Redis 장애로 실패한 회사별 캐시 무효화를 요청 처리와 분리해 다시 시도합니다.
    @Scheduled(
            initialDelayString =
                    "${bidding.notice-cache.retry.initial-delay-ms:30000}",
            fixedDelayString =
                    "${bidding.notice-cache.retry.fixed-delay-ms:30000}",
            scheduler = "biddingTaskScheduler"
    )
    public void retryPendingInvalidations() {
        int succeededCount = 0;
        for (BidNoticeCacheInvalidationRetryQueue.RetryEntry entry
                : retryQueue.snapshot(batchSize)) {
            try {
                if (cachePort.invalidate(entry.companyId())) {
                    retryQueue.removeIfUnchanged(entry);
                    succeededCount++;
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "Bidding notice cache invalidation retry failed. companyId={}",
                        entry.companyId(),
                        exception
                );
            }
        }
        if (succeededCount > 0) {
            log.info(
                    "Bidding notice cache invalidation retry completed. succeededCount={} pendingCount={}",
                    succeededCount,
                    retryQueue.size()
            );
        }
    }
}

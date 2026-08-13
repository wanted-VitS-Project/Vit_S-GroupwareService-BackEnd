package com.group3.vitamins.bidding.bidreview.infrastructure.scheduling;

import com.group3.vitamins.bidding.bidreview.application.service.BidReviewCleanupExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidReviewCleanupExecutionScheduler {

    private final BidReviewCleanupExecutionService cleanupExecutionService;

    private final String lockOwner = "bidding-review-cleanup-" + UUID.randomUUID();

    @Value("${bidding.review.cleanup.batch-size:50}")
    private int batchSize;

    @Scheduled(
            initialDelayString = "${bidding.review.cleanup.initial-delay-ms:8000}",
            fixedDelayString = "${bidding.review.cleanup.fixed-delay-ms:5000}"
    )
    public void cleanupPendingReviews() {
        try {
            int cleanedCount = cleanupExecutionService.cleanupBatch(lockOwner, batchSize);
            if (cleanedCount > 0) {
                log.info("Bid review cleanup executed for {} reviews.", cleanedCount);
            }
        } catch (RuntimeException exception) {
            log.error("Bid review cleanup scheduler failed. errorType={}", exception.getClass().getSimpleName());
        }
    }
}

package com.group3.vitamins.bidding.bidreview.infrastructure.scheduling;

import com.group3.vitamins.bidding.bidreview.application.service.BidReviewExpiryScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidReviewExpiryScanScheduler {

    private final BidReviewExpiryScanService scanService;

    @Value("${bidding.review.expiry-scan.batch-size:50}")
    private int batchSize;

    @Scheduled(
            initialDelayString = "${bidding.review.expiry-scan.initial-delay-ms:10000}",
            fixedDelayString = "${bidding.review.expiry-scan.fixed-delay-ms:60000}"
    )
    public void scanExpiredReviews() {
        try {
            int claimedCount = scanService.scanBatch(batchSize);
            if (claimedCount > 0) {
                log.info("Bid review expiry scan claimed {} reviews for cleanup.", claimedCount);
            }
        } catch (RuntimeException exception) {
            log.error("Bid review expiry scan failed. errorType={}", exception.getClass().getSimpleName());
        }
    }
}
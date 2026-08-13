package com.group3.vitamins.bidding.bidreview.infrastructure.scheduling;

import com.group3.vitamins.bidding.bidreview.application.service.BidReviewOutboxPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidReviewOutboxPublisherScheduler {

    private final BidReviewOutboxPublishService publishService;

    private final String lockOwner = "bidding-review-" + UUID.randomUUID();

    @Value("${bidding.review.outbox.batch-size:50}")
    private int batchSize;

    // 커밋된 입찰 문서 검토 Outbox를 Redis Stream으로 전달합니다.
    @Scheduled(
            initialDelayString = "${bidding.review.outbox.initial-delay-ms:5000}",
            fixedDelayString = "${bidding.review.outbox.fixed-delay-ms:1000}"
    )
    public void publishPendingOutboxes() {
        try {
            int publishedCount = publishService.publishBatch(lockOwner, batchSize);

            if (publishedCount > 0) {
                log.info("Bid review outbox batch published. publishedCount={}", publishedCount);
            }
        } catch (RuntimeException exception) {
            log.error("Bid review outbox scheduler failed. errorType={}", exception.getClass().getSimpleName());
        }
    }
}
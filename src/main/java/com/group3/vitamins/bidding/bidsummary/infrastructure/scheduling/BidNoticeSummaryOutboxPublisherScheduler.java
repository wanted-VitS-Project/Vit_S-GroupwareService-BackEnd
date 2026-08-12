package com.group3.vitamins.bidding.bidsummary.infrastructure.scheduling;

import com.group3.vitamins.bidding.bidsummary.application.service.BidNoticeSummaryOutboxPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidNoticeSummaryOutboxPublisherScheduler {

    private final BidNoticeSummaryOutboxPublishService publishService;

    private final String lockOwner =
            "bidding-summary-" + UUID.randomUUID();

    @Value("${bidding.summary.outbox.batch-size:50}")
    private int batchSize;

    // 커밋된 입찰 AI 요약 Outbox를 Redis Stream으로 전달합니다.
    @Scheduled(
            initialDelayString =
                    "${bidding.summary.outbox.initial-delay-ms:5000}",
            fixedDelayString =
                    "${bidding.summary.outbox.fixed-delay-ms:1000}"
    )
    public void publishPendingOutboxes() {
        try {
            int publishedCount =
                    publishService.publishBatch(lockOwner, batchSize);

            if (publishedCount > 0) {
                log.info(
                        "Bidding summary outbox batch published. publishedCount={}",
                        publishedCount
                );
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Bidding summary outbox scheduler failed. errorType={}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}
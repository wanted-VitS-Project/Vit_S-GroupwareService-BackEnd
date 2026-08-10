package com.group3.vitamins.bidding.collectionrun.infrastructure.scheduling;

import com.group3.vitamins.bidding.collectionrun.application.service.CollectionRunOutboxPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionRunOutboxPublisherScheduler {

    private final CollectionRunOutboxPublishService publishService;
    private final String lockOwner =
            "bidding-collection-" + UUID.randomUUID();

    @Value("${bidding.collection.outbox.batch-size:50}")
    private int batchSize;

    // 커밋된 입찰 수집 Outbox를 주기적으로 Redis Stream에 전달합니다.
    @Scheduled(
            initialDelayString =
                    "${bidding.collection.outbox.initial-delay-ms:5000}",
            fixedDelayString =
                    "${bidding.collection.outbox.fixed-delay-ms:1000}"
    )
    public void publishPendingOutboxes() {
        try {
            int publishedCount =
                    publishService.publishBatch(lockOwner, batchSize);

            if (publishedCount > 0) {
                log.info(
                        "Bidding collection outbox batch published. publishedCount={}",
                        publishedCount
                );
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Bidding collection outbox scheduler failed. errorType={}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}

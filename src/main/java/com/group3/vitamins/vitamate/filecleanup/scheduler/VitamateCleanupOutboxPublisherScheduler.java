package com.group3.vitamins.vitamate.filecleanup.infrastructure.scheduling;

import com.group3.vitamins.vitamate.filecleanup.application.service.VitamateCleanupOutboxPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VitamateCleanupOutboxPublisherScheduler {

    private final VitamateCleanupOutboxPublishService publishService;

    private final String lockOwner =
            "vitamate-cleanup-" + UUID.randomUUID();

    @Value("${vitamate.cleanup.outbox.batch-size:50}")
    private int batchSize;

    // 미발행 Cleanup Outbox를 주기적으로 조회해 Redis Stream에 발행합니다.
    @Scheduled(
            initialDelayString =
                    "${vitamate.cleanup.outbox.initial-delay-ms:5000}",
            fixedDelayString =
                    "${vitamate.cleanup.outbox.fixed-delay-ms:1000}"
    )
    public void publishPendingOutboxes() {
        try {
            int publishedCount =
                    publishService.publishBatch(lockOwner, batchSize);

            if (publishedCount > 0) {
                log.info(
                        "Vitamate cleanup outbox batch published. publishedCount={}",
                        publishedCount
                );
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Vitamate cleanup outbox scheduler failed. errorType={}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}
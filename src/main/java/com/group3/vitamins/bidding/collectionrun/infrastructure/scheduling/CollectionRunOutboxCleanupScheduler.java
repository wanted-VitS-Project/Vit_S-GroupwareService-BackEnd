package com.group3.vitamins.bidding.collectionrun.infrastructure.scheduling;

import com.group3.vitamins.bidding.collectionrun.application.service.CollectionRunOutboxCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionRunOutboxCleanupScheduler {

    private final CollectionRunOutboxCleanupService cleanupService;

    // 발행 완료된 지 오래된 입찰 수집 Outbox 행을 매일 새벽에 정리합니다.
    @Scheduled(cron = "${bidding.collection.outbox.cleanup-cron:0 0 4 * * *}")
    public void cleanupPublishedOutboxes() {
        try {
            int deletedCount = cleanupService.cleanupPublished();
            if (deletedCount > 0) {
                log.info(
                        "Bidding collection outbox cleanup executed. deletedCount={}",
                        deletedCount
                );
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Bidding collection outbox cleanup scheduler failed. errorType={}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}

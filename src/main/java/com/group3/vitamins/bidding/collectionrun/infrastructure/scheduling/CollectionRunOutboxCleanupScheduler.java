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
    // ⚠️ zone을 생략하면 스케줄러 기본 시간대(서버 JVM 시간대)를 따른다 - 배포 환경이 UTC면
    // "새벽 4시"가 실제로는 한국 시간 낮 시간에 돌아버릴 수 있어 Asia/Seoul로 명시한다.
    @Scheduled(
            cron = "${bidding.collection.outbox.cleanup-cron:0 0 4 * * *}",
            zone = "${bidding.collection.outbox.cleanup-cron-zone:Asia/Seoul}"
    )
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

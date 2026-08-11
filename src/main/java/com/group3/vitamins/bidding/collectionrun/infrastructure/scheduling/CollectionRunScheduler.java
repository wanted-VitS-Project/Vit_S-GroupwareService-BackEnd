package com.group3.vitamins.bidding.collectionrun.infrastructure.scheduling;

import com.group3.vitamins.bidding.collectionrun.application.service.ScheduledCollectionRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionRunScheduler {

    private final ScheduledCollectionRunService scheduledRunService;

    @Value("${bidding.collection.schedule.batch-size:50}")
    private int batchSize;

    // 매분 실행 시각이 된 조건을 확인하여 자동 수집 실행을 생성합니다.
    @Scheduled(
            cron = "${bidding.collection.schedule.cron:0 * * * * *}",
            zone = "Asia/Seoul"
    )
    public void createScheduledRuns() {
        try {
            int createdCount = scheduledRunService.createDueRuns(batchSize);
            log.info(
                    "Scheduled bidding collection scan completed. createdCount={}",
                    createdCount
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Scheduled bidding collection failed. errorType={}",
                    exception.getClass().getSimpleName(),
                    exception
            );
        }
    }
}

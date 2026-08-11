package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.support.CollectionScheduleCalculator;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectionrun.application.port.ScheduledCollectionConditionPort;
import com.group3.vitamins.bidding.collectionrun.application.support.CollectionRunCreator;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
import com.group3.vitamins.bidding.collectionrun.domain.repository.CollectionRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledCollectionRunService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final ScheduledCollectionConditionPort scheduledConditionPort;
    private final CollectionRunRepository runRepository;
    private final CollectionRunCreator runCreator;
    private final CollectionScheduleCalculator scheduleCalculator;
    private final Clock clock;

    // 실행 시각이 된 조건을 점유하고 자동 수집 실행과 다음 예약을 함께 저장합니다.
    @Transactional
    public int createDueRuns(int batchSize) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), SEOUL_ZONE);
        List<CollectionCondition> conditions =
                scheduledConditionPort.claimDueConditions(now, batchSize);

        int createdCount = 0;
        for (CollectionCondition condition : conditions) {
            LocalDateTime scheduledAt = condition.getNextRunAt();
            LocalDateTime nextRunAt = scheduleCalculator.nextRunAt(
                    condition.getScheduleType(),
                    condition.getScheduledTime(),
                    scheduledAt,
                    now
            );

            if (runRepository.existsActiveByConditionId(
                    condition.getConditionId()
            )) {
                scheduledConditionPort.advanceSchedule(
                        condition.getConditionId(), nextRunAt, now
                );
                log.info(
                        "Scheduled collection skipped because active run exists. conditionId={} nextRunAt={}",
                        condition.getConditionId(),
                        nextRunAt
                );
                continue;
            }

            runCreator.create(
                    condition,
                    condition.getCompanyId(),
                    CollectionRunTriggerType.SCHEDULED,
                    null,
                    now
            );
            scheduledConditionPort.recordScheduledRun(
                    condition.getConditionId(), scheduledAt, nextRunAt, now
            );
            createdCount++;
        }

        return createdCount;
    }
}

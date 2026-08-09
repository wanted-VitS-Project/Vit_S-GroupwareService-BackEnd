package com.group3.vitamins.vitamate.filecleanup.application.service;

import com.group3.vitamins.vitamate.filecleanup.application.model.ClaimedVitamateCleanupOutbox;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupJobPublisherPort;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupOutboxStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VitamateCleanupOutboxPublishService {

    private static final int LOCK_SECONDS = 30;

    private final VitamateCleanupOutboxStorePort outboxStorePort;
    private final VitamateCleanupJobPublisherPort jobPublisherPort;
    private final Clock clock;

    // 발행 가능한 Outbox를 점유한 뒤 Redis Stream에 순서대로 발행합니다.
    public int publishBatch(String lockOwner, int batchSize) {
        validateRequest(lockOwner, batchSize);

        LocalDateTime claimedAt = LocalDateTime.now(clock);

        List<ClaimedVitamateCleanupOutbox> outboxes =
                outboxStorePort.claimPublishable(
                        lockOwner,
                        batchSize,
                        claimedAt,
                        claimedAt.plusSeconds(LOCK_SECONDS)
                );

        int publishedCount = 0;

        for (ClaimedVitamateCleanupOutbox outbox : outboxes) {
            if (publishOne(outbox, lockOwner)) {
                publishedCount++;
            }
        }

        return publishedCount;
    }

    // 단일 Outbox를 발행하고 결과를 DB에 반영합니다.
    private boolean publishOne(
            ClaimedVitamateCleanupOutbox outbox,
            String lockOwner
    ) {
        try {
            jobPublisherPort.publish(outbox);

            outboxStorePort.markPublished(
                    outbox.outboxId(),
                    lockOwner,
                    LocalDateTime.now(clock)
            );

            return true;
        } catch (RuntimeException exception) {
            handlePublishFailure(outbox, lockOwner, exception);
            return false;
        }
    }

    // 발행 실패 시 잠금을 해제하고 다음 재시도 시각을 저장합니다.
    private void handlePublishFailure(
            ClaimedVitamateCleanupOutbox outbox,
            String lockOwner,
            RuntimeException exception
    ) {
        LocalDateTime nextAvailableAt = LocalDateTime.now(clock)
                .plusSeconds(retryDelaySeconds(outbox.publishAttemptCount()));

        outboxStorePort.markPublishFailed(
                outbox.outboxId(),
                lockOwner,
                exception.getClass().getSimpleName(),
                nextAvailableAt
        );

        log.error(
                "Vitamate cleanup job publish failed. cleanupJobId={}, attemptId={}, publishAttemptCount={}, errorType={}",
                outbox.cleanupJobId(),
                outbox.attemptId(),
                outbox.publishAttemptCount(),
                exception.getClass().getSimpleName()
        );
    }

    // 발행 실패가 반복될수록 재시도 간격을 늘립니다.
    private long retryDelaySeconds(int publishAttemptCount) {
        return switch (publishAttemptCount) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 60;
            default -> 300;
        };
    }

    // Scheduler 설정 오류로 무제한 조회가 발생하지 않게 입력을 검증합니다.
    private void validateRequest(String lockOwner, int batchSize) {
        if (lockOwner == null || lockOwner.isBlank()) {
            throw new IllegalArgumentException(
                    "Outbox lockOwner는 비어 있을 수 없습니다."
            );
        }

        if (batchSize <= 0 || batchSize > 100) {
            throw new IllegalArgumentException(
                    "Outbox batchSize는 1 이상 100 이하여야 합니다."
            );
        }
    }
}
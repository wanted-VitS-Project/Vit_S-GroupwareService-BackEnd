package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunJobPublisherPort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskDlqPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CollectionRunOutboxPublishService {

    private final CollectionRunOutboxStorePort outboxStorePort;
    private final CollectionRunJobPublisherPort jobPublisherPort;
    private final CollectionRunTaskDlqPort taskDlqPort;
    private final Clock clock;
    private final int lockSeconds;

    public CollectionRunOutboxPublishService(
            CollectionRunOutboxStorePort outboxStorePort,
            CollectionRunJobPublisherPort jobPublisherPort,
            CollectionRunTaskDlqPort taskDlqPort,
            Clock clock,
            @Value("${bidding.collection.outbox.lock-seconds:300}") int lockSeconds
    ) {
        if (lockSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Outbox 잠금 시간은 1초 이상이어야 합니다."
            );
        }
        this.outboxStorePort = outboxStorePort;
        this.jobPublisherPort = jobPublisherPort;
        this.taskDlqPort = taskDlqPort;
        this.clock = clock;
        this.lockSeconds = lockSeconds;
    }

    // 발행 가능한 Outbox를 점유한 뒤 Redis Stream으로 순서대로 발행합니다.
    public int publishBatch(String lockOwner, int batchSize) {
        validateRequest(lockOwner, batchSize);

        LocalDateTime claimedAt = LocalDateTime.now(clock);
        List<ClaimedCollectionRunOutbox> outboxes =
                outboxStorePort.claimPublishable(
                        lockOwner,
                        batchSize,
                        claimedAt,
                        claimedAt.plusSeconds(lockSeconds)
                );

        int publishedCount = 0;
        for (ClaimedCollectionRunOutbox outbox : outboxes) {
            if (publishOne(outbox, lockOwner)) {
                publishedCount++;
            }
        }
        return publishedCount;
    }

    private boolean publishOne(
            ClaimedCollectionRunOutbox outbox,
            String lockOwner
    ) {
        try {
            publishToTargetStream(outbox);
            outboxStorePort.markPublished(
                    outbox.outboxId(),
                    lockOwner,
                    LocalDateTime.now(clock)
            );
            return true;
        } catch (RuntimeException exception) {
            LocalDateTime nextAvailableAt = LocalDateTime.now(clock)
                    .plusSeconds(retryDelaySeconds(outbox.publishAttemptCount()));

            outboxStorePort.markPublishFailed(
                    outbox.outboxId(),
                    lockOwner,
                    "REDIS_PUBLISH_FAILED",
                    nextAvailableAt
            );

            log.error(
                    "Bidding collection job publish failed. runId={}, attemptId={}, publishAttemptCount={}, errorType={}",
                    outbox.runId(),
                    outbox.attemptId(),
                    outbox.publishAttemptCount(),
                    "REDIS_PUBLISH_FAILED"
            );
            return false;
        }
    }

    // Outbox 이벤트 종류에 맞는 Redis Stream으로 발행합니다.
    private void publishToTargetStream(ClaimedCollectionRunOutbox outbox) {
        if (outbox.isTaskFailureEvent()) {
            taskDlqPort.publish(outbox.taskFailure());
            return;
        }
        jobPublisherPort.publish(outbox);
    }

    private long retryDelaySeconds(int publishAttemptCount) {
        return switch (publishAttemptCount) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 60;
            default -> 300;
        };
    }

    private void validateRequest(String lockOwner, int batchSize) {
        if (lockOwner == null || lockOwner.isBlank()) {
            throw new IllegalArgumentException(
                    "Outbox 잠금 소유자는 비어 있을 수 없습니다."
            );
        }
        if (batchSize <= 0 || batchSize > 100) {
            throw new IllegalArgumentException(
                    "Outbox 배치 크기는 1 이상 100 이하여야 합니다."
            );
        }
    }
}

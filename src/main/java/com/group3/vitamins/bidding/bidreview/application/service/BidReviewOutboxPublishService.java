package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.model.ClaimedBidReviewOutbox;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewJobPublisherPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewOutboxStorePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class BidReviewOutboxPublishService {

    private final BidReviewOutboxStorePort outboxStorePort;
    private final BidReviewJobPublisherPort jobPublisherPort;
    private final Clock clock;
    private final int lockSeconds;

    public BidReviewOutboxPublishService(
            BidReviewOutboxStorePort outboxStorePort,
            BidReviewJobPublisherPort jobPublisherPort,
            Clock clock,
            @Value("${bidding.review.outbox.lock-seconds:300}")
            int lockSeconds
    ) {
        if (lockSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Outbox 점유 시간은 1초 이상이어야 합니다."
            );
        }

        this.outboxStorePort = outboxStorePort;
        this.jobPublisherPort = jobPublisherPort;
        this.clock = clock;
        this.lockSeconds = lockSeconds;
    }

    // 발행 가능한 Outbox를 점유하여 Redis Stream으로 전달합니다.
    public int publishBatch(String lockOwner, int batchSize) {
        validateRequest(lockOwner, batchSize);

        int publishedCount = 0;

        for (int index = 0; index < batchSize; index++) {
            LocalDateTime claimedAt = LocalDateTime.now(clock);
            List<ClaimedBidReviewOutbox> outboxes =
                    outboxStorePort.claimPublishable(
                            lockOwner,
                            1,
                            claimedAt,
                            claimedAt.plusSeconds(lockSeconds)
                    );

            if (outboxes.isEmpty()) {
                break;
            }

            if (publishOne(outboxes.get(0), lockOwner)) {
                publishedCount++;
            }
        }

        return publishedCount;
    }

    private boolean publishOne(
            ClaimedBidReviewOutbox outbox,
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
            LocalDateTime failedAt = LocalDateTime.now(clock);
            LocalDateTime nextAvailableAt = failedAt.plusSeconds(
                    retryDelaySeconds(outbox.publishAttemptCount())
            );

            outboxStorePort.markPublishFailed(
                    outbox.outboxId(),
                    lockOwner,
                    "REDIS_PUBLISH_FAILED",
                    nextAvailableAt,
                    failedAt
            );

            log.error(
                    "Bid review outbox publish failed. reviewId={}, eventType={}, attemptId={}, publishAttemptCount={}",
                    outbox.reviewId(),
                    outbox.eventType(),
                    outbox.attemptId(),
                    outbox.publishAttemptCount()
            );
            return false;
        }
    }

    private long retryDelaySeconds(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 60;
            default -> 300;
        };
    }

    private void validateRequest(String lockOwner, int batchSize) {
        if (lockOwner == null || lockOwner.isBlank()) {
            throw new IllegalArgumentException(
                    "Outbox 점유 서버 식별자는 필수입니다."
            );
        }

        if (batchSize <= 0 || batchSize > 100) {
            throw new IllegalArgumentException(
                    "Outbox 배치 크기는 1 이상 100 이하여야 합니다."
            );
        }
    }
}

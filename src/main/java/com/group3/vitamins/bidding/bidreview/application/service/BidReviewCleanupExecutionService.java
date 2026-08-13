package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCleanupStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidReviewCleanupExecutionService {

    private static final int LOCK_MINUTES = 5;

    private final BidReviewCleanupStorePort cleanupStorePort;
    private final Clock clock;

    // 정리 대기 Outbox를 점유해 실행합니다. 실제로 정리 완료한 건수를 반환합니다.
    public int cleanupBatch(String lockOwner, int batchSize) {
        int cleanedCount = 0;

        for (int index = 0; index < batchSize; index++) {
            LocalDateTime now = LocalDateTime.now(clock);
            Optional<Long> outboxId = cleanupStorePort.claimNext(
                    lockOwner, now, now.plusMinutes(LOCK_MINUTES)
            );

            if (outboxId.isEmpty()) {
                break;
            }

            if (cleanupOne(outboxId.get(), lockOwner)) {
                cleanedCount++;
            }
        }

        return cleanedCount;
    }

    private boolean cleanupOne(Long outboxId, String lockOwner) {
        try {
            cleanupStorePort.execute(outboxId, lockOwner, LocalDateTime.now(clock));
            return true;
        } catch (RuntimeException exception) {
            LocalDateTime failedAt = LocalDateTime.now(clock);
            cleanupStorePort.markFailed(outboxId, lockOwner, failedAt.plusMinutes(1), failedAt);
            log.error("Bid review cleanup execution failed. outboxId={}", outboxId, exception);
            return false;
        }
    }
}
package com.group3.vitamins.bidding.bidreview.application.port;

import com.group3.vitamins.bidding.bidreview.application.model.ClaimedBidReviewOutbox;

import java.time.LocalDateTime;
import java.util.List;

public interface BidReviewOutboxStorePort {

    // 발행 가능한 Outbox를 현재 서버가 일정 시간 동안 점유합니다.
    List<ClaimedBidReviewOutbox> claimPublishable(
            String lockOwner,
            int batchSize,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    );

    // 현재 서버가 점유한 Outbox의 Redis 발행 성공을 기록합니다.
    void markPublished(
            Long outboxId,
            String lockOwner,
            LocalDateTime publishedAt
    );

    // 발행 실패를 기록하고 다음 재시도 가능 시각을 설정합니다.
    void markPublishFailed(
            Long outboxId,
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt,
            LocalDateTime failedAt
    );
}

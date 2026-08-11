package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskFailure;

import java.time.LocalDateTime;
import java.util.List;

public interface CollectionRunOutboxStorePort {

    // 수집 실행과 함께 최초 PENDING Outbox를 저장합니다.
    void savePending(CollectionRunOutbox.Pending outbox);

    // Task 영구 실패와 같은 트랜잭션에서 DLQ 발행 대기 이벤트를 저장합니다.
    void saveTaskFailurePending(
            String eventId,
            CollectionRunTaskFailure failure,
            LocalDateTime createdAt
    );

    // 발행 가능한 Outbox를 현재 서버가 일정 시간 점유합니다.
    List<ClaimedCollectionRunOutbox> claimPublishable(
            String lockOwner,
            int batchSize,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    );

    // Redis 발행에 성공한 Outbox를 완료 처리합니다.
    void markPublished(
            Long outboxId,
            String lockOwner,
            LocalDateTime publishedAt
    );

    // Redis 발행 실패를 기록하고 다음 발행 가능 시각을 설정합니다.
    void markPublishFailed(
            Long outboxId,
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt
    );
}

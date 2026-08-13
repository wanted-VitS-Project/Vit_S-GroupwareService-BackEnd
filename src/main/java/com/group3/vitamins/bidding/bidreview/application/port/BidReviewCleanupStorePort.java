package com.group3.vitamins.bidding.bidreview.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BidReviewCleanupStorePort {

    // 정리 대기 Outbox 하나를 점유한다. 없으면 empty.
    Optional<Long> claimNext(String lockOwner, LocalDateTime now, LocalDateTime lockExpiresAt);

    // 점유한 정리 작업을 실행한다 — 임시 S3 객체 삭제, 문서·검토 상태 반영, Outbox 완료 처리.
    void execute(Long outboxId, String lockOwner, LocalDateTime now);

    // 실행 실패 시 재시도 지연시각을 기록한다.
    void markFailed(Long outboxId, String lockOwner, LocalDateTime nextAvailableAt, LocalDateTime now);
}
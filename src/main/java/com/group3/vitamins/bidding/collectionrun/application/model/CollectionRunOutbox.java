package com.group3.vitamins.bidding.collectionrun.application.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class CollectionRunOutbox {

    private CollectionRunOutbox() {
    }

    // Redis Stream 발행 여부를 나타냅니다.
    public enum PublishStatus {
        PENDING,
        PUBLISHED
    }

    // 수집 실행과 같은 트랜잭션에서 저장할 Outbox 정보입니다.
    public record Pending(
            String eventId,
            Long runId,
            Long conditionId,
            Long companyId,
            String attemptId,
            String eventType,
            int retryCount,
            LocalDateTime createdAt
    ) {
        public Pending {
            Objects.requireNonNull(eventId, "이벤트 ID는 필수입니다.");
            Objects.requireNonNull(runId, "수집 실행 ID는 필수입니다.");
            Objects.requireNonNull(conditionId, "수집 조건 ID는 필수입니다.");
            Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
            Objects.requireNonNull(attemptId, "처리 시도 ID는 필수입니다.");
            Objects.requireNonNull(eventType, "이벤트 유형은 필수입니다.");
            Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");

            if (retryCount < 0) {
                throw new IllegalArgumentException(
                        "재시도 횟수는 0 이상이어야 합니다."
                );
            }
        }
    }
}
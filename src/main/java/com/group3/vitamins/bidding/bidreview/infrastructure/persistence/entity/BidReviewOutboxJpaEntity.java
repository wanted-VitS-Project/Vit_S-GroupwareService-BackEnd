package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "bid_review_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidReviewOutboxJpaEntity {

    private static final int MAX_PUBLISH_ATTEMPT_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_review_outbox_id")
    private Long outboxId;

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false,
            length = 36,
            columnDefinition = "CHAR(36)"
    )
    private String eventId;

    @Column(name = "bid_review_id", nullable = false, updatable = false)
    private Long reviewId;

    @Column(
            name = "attempt_id",
            nullable = false,
            updatable = false,
            length = 36,
            columnDefinition = "CHAR(36)"
    )
    private String attemptId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "payload",
            nullable = false,
            updatable = false,
            columnDefinition = "JSON"
    )
    private JsonNode payload;

    @Column(name = "publish_status", nullable = false, length = 20)
    private String publishStatus;

    @Column(name = "publish_attempt_count", nullable = false)
    private int publishAttemptCount;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "lock_owner", length = 100)
    private String lockOwner;

    @Column(name = "lock_expires_at")
    private LocalDateTime lockExpiresAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Python Worker에 전달할 검토 요청 이벤트를 발행 대기 상태로 만듭니다.
    public static BidReviewOutboxJpaEntity pending(
            String eventId,
            Long reviewId,
            String attemptId,
            String eventType,
            JsonNode payload,
            LocalDateTime now
    ) {
        BidReviewOutboxJpaEntity entity = new BidReviewOutboxJpaEntity();

        entity.eventId = eventId;
        entity.reviewId = reviewId;
        entity.attemptId = attemptId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.publishStatus = "PENDING";
        entity.publishAttemptCount = 0;
        entity.availableAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    // 재시도처럼 발행 가능 시각을 생성 시각보다 뒤로 미뤄야 할 때 쓴다(지연 백오프).
    public static BidReviewOutboxJpaEntity pending(
            String eventId,
            Long reviewId,
            String attemptId,
            String eventType,
            JsonNode payload,
            LocalDateTime availableAt,
            LocalDateTime now
    ) {
        BidReviewOutboxJpaEntity entity =
                pending(eventId, reviewId, attemptId, eventType, payload, now);
        entity.availableAt = availableAt;
        return entity;
    }

    // 현재 서버가 Outbox 발행 작업을 일정 시간 점유합니다.
    public void claim(
            String lockOwner,
            LocalDateTime lockExpiresAt,
            LocalDateTime now
    ) {
        this.lockOwner = lockOwner;
        this.lockExpiresAt = lockExpiresAt;
        this.publishAttemptCount += 1;
        this.updatedAt = now;
    }

    // Redis 발행 성공을 기록합니다.
    public boolean markPublished(
            String expectedLockOwner,
            LocalDateTime now
    ) {
        if (!isOwnedBy(expectedLockOwner)) {
            return false;
        }

        this.publishStatus = "PUBLISHED";
        this.publishedAt = now;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = null;
        this.updatedAt = now;
        return true;
    }

    // Redis 발행 실패를 기록하고 재시도 또는 최종 실패로 전환합니다.
    public boolean markPublishFailed(
            String expectedLockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt,
            LocalDateTime now
    ) {
        if (!isOwnedBy(expectedLockOwner)) {
            return false;
        }

        this.publishStatus =
                publishAttemptCount >= MAX_PUBLISH_ATTEMPT_COUNT
                        ? "FAILED"
                        : "PENDING";

        this.availableAt = nextAvailableAt;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = truncate(errorMessage);
        this.updatedAt = now;
        return true;
    }

    private boolean isOwnedBy(String expectedLockOwner) {
        return "PENDING".equals(publishStatus)
                && lockOwner != null
                && lockOwner.equals(expectedLockOwner);
    }

    private String truncate(String message) {
        if (message == null || message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}

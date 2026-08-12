package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity;

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
@Table(name = "bid_notice_summary_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidNoticeSummaryOutboxJpaEntity {

    private static final int MAX_PUBLISH_ATTEMPT_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_notice_summary_outbox_id")
    private Long outboxId;

    @Column(name = "event_id", nullable = false, length = 36,
            columnDefinition = "CHAR(36)", updatable = false)
    private String eventId;

    @Column(name = "bid_notice_summary_id", nullable = false, updatable = false)
    private Long summaryId;

    @Column(name = "attempt_id", nullable = false, length = 36,
            columnDefinition = "CHAR(36)", updatable = false)
    private String attemptId;

    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false,
            columnDefinition = "JSON", updatable = false)
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

    // Redis 발행을 기다리는 PENDING Outbox를 생성합니다.
    public static BidNoticeSummaryOutboxJpaEntity pending(
            String eventId,
            Long summaryId,
            String attemptId,
            String eventType,
            JsonNode payload,
            LocalDateTime now
    ) {
        return pending(
                eventId,
                summaryId,
                attemptId,
                eventType,
                payload,
                now,
                now
        );
    }

    // 지정한 시각 이후 발행할 PENDING Outbox를 생성합니다.
    public static BidNoticeSummaryOutboxJpaEntity pending(
            String eventId,
            Long summaryId,
            String attemptId,
            String eventType,
            JsonNode payload,
            LocalDateTime availableAt,
            LocalDateTime now
    ) {
        BidNoticeSummaryOutboxJpaEntity entity =
                new BidNoticeSummaryOutboxJpaEntity();

        entity.eventId = eventId;
        entity.summaryId = summaryId;
        entity.attemptId = attemptId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.publishStatus = "PENDING";
        entity.publishAttemptCount = 0;
        entity.availableAt = availableAt;
        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    // 현재 서버가 Outbox 발행 권한을 일정 시간 점유합니다.
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

    // 현재 점유 서버가 Redis 발행 성공을 기록합니다.
    public boolean markPublished(
            String lockOwner,
            LocalDateTime publishedAt
    ) {
        if (!isOwnedBy(lockOwner)) {
            return false;
        }

        this.publishStatus = "PUBLISHED";
        this.publishedAt = publishedAt;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = null;
        this.updatedAt = publishedAt;
        return true;
    }

    // 발행 실패를 기록하고 재시도 또는 최종 실패 상태로 전이합니다.
    public boolean markPublishFailed(
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt,
            LocalDateTime failedAt
    ) {
        if (!isOwnedBy(lockOwner)) {
            return false;
        }

        this.publishStatus = publishAttemptCount >= MAX_PUBLISH_ATTEMPT_COUNT
                ? "FAILED"
                : "PENDING";
        this.availableAt = nextAvailableAt;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = truncate(errorMessage);
        this.updatedAt = failedAt;
        return true;
    }

    // 역직렬화할 수 없는 메시지는 재시도하지 않고 종료합니다.
    public void markInvalidPayload(LocalDateTime now) {
        this.publishStatus = "FAILED";
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = "INVALID_OUTBOX_PAYLOAD";
        this.updatedAt = now;
    }

    private boolean isOwnedBy(String lockOwner) {
        return "PENDING".equals(publishStatus)
                && this.lockOwner != null
                && this.lockOwner.equals(lockOwner);
    }

    private String truncate(String message) {
        if (message == null || message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}

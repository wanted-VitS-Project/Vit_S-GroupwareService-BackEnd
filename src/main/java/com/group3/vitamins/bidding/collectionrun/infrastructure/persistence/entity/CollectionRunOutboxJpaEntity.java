package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunOutbox;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "crawl_run_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionRunOutboxJpaEntity {

    private static final int MAX_PUBLISH_ATTEMPT_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crawl_run_outbox_id")
    private Long crawlRunOutboxId;

    @Column(
            name = "event_id",
            nullable = false,
            length = 36,
            columnDefinition = "CHAR(36)",
            updatable = false
    )
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crawl_run_id", nullable = false, updatable = false)
    private CollectionRunJpaEntity crawlRun;

    @Column(
            name = "attempt_id",
            nullable = false,
            length = 36,
            columnDefinition = "CHAR(36)",
            updatable = false
    )
    private String attemptId;

    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSON", updatable = false)
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 20)
    private CollectionRunOutbox.PublishStatus publishStatus;

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

    // 수집 실행과 함께 저장할 최초 PENDING Outbox를 생성합니다.
    public static CollectionRunOutboxJpaEntity pending(
            String eventId,
            CollectionRunJpaEntity crawlRun,
            String attemptId,
            String eventType,
            JsonNode payload,
            LocalDateTime now
    ) {
        CollectionRunOutboxJpaEntity entity =
                new CollectionRunOutboxJpaEntity();

        entity.eventId = eventId;
        entity.crawlRun = crawlRun;
        entity.attemptId = attemptId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.publishStatus = CollectionRunOutbox.PublishStatus.PENDING;
        entity.publishAttemptCount = 0;
        entity.availableAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    // 현재 서버가 발행 작업을 일정 시간 점유합니다.
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

    // 현재 점유 서버가 Redis 발행을 완료한 경우에만 상태를 변경합니다.
    public boolean markPublished(
            String lockOwner,
            LocalDateTime publishedAt
    ) {
        if (!isOwnedBy(lockOwner)) {
            return false;
        }

        this.publishStatus = CollectionRunOutbox.PublishStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = null;
        this.updatedAt = publishedAt;

        return true;
    }

    // 발행 실패 정보를 저장하고 다음 재시도 시각까지 잠금을 해제합니다.
    public boolean markPublishFailed(
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt,
            LocalDateTime now
    ) {
        if (!isOwnedBy(lockOwner)) {
            return false;
        }

        this.publishStatus = publishAttemptCount >= MAX_PUBLISH_ATTEMPT_COUNT
                ? CollectionRunOutbox.PublishStatus.FAILED
                : CollectionRunOutbox.PublishStatus.PENDING;
        this.availableAt = nextAvailableAt;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = truncate(errorMessage);
        this.updatedAt = now;

        return true;
    }

    // 역직렬화할 수 없는 Outbox는 재시도하지 않고 안전한 실패 코드로 종료합니다.
    public void markInvalidPayload(LocalDateTime now) {
        this.publishStatus = CollectionRunOutbox.PublishStatus.FAILED;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = "INVALID_OUTBOX_PAYLOAD";
        this.updatedAt = now;
    }

    // 다른 서버가 점유한 Outbox를 잘못 변경하지 못하게 확인합니다.
    private boolean isOwnedBy(String lockOwner) {
        return publishStatus == CollectionRunOutbox.PublishStatus.PENDING
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

package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;

import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupOutbox;
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

// Redis Stream 발행을 보장하기 위한 비타메이트 outbox 이벤트를 저장합니다.
@Getter
@Entity
@Table(name = "vitamate_cleanup_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitamateCleanupOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cleanup_outbox_id")
    private Long cleanupOutboxId;

    @Column(
            name = "event_id",
            nullable = false,
            length = 36,
            columnDefinition = "CHAR(36)",
            updatable = false
    )
    private String eventId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cleanup_job_id", nullable = false, updatable = false)
    private VitamateCleanupJobEntity cleanupJob;

    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON", updatable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 20)
    private VitamateCleanupOutbox.PublishStatus publishStatus;

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

    // 새 cleanup job을 Redis에 전달할 최초 PENDING 이벤트를 생성합니다.
    public static VitamateCleanupOutboxEntity pending(
            String eventId,
            VitamateCleanupJobEntity cleanupJob,
            String eventType,
            JsonNode payload,
            LocalDateTime now
    ) {
        VitamateCleanupOutboxEntity entity = new VitamateCleanupOutboxEntity();
        entity.eventId = eventId;
        entity.cleanupJob = cleanupJob;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.publishStatus = VitamateCleanupOutbox.PublishStatus.PENDING;
        entity.publishAttemptCount = 0;
        entity.availableAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    // 발행할 Outbox를 현재 Spring 인스턴스가 점유합니다.
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

    // Redis 발행 성공 상태로 변경합니다.
    public boolean markPublished(
            String lockOwner,
            LocalDateTime publishedAt
    ) {
        if (!isOwnedBy(lockOwner)) {
            return false;
        }

        this.publishStatus = VitamateCleanupOutbox.PublishStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = null;
        this.updatedAt = publishedAt;
        return true;
    }

    // Redis 발행 실패 후 재시도할 수 있도록 잠금을 해제합니다.
    public boolean markPublishFailed(
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt,
            LocalDateTime now
    ) {
        if (!isOwnedBy(lockOwner)) {
            return false;
        }

        this.availableAt = nextAvailableAt;
        this.lockOwner = null;
        this.lockExpiresAt = null;
        this.lastErrorMessage = truncate(errorMessage);
        this.updatedAt = now;
        return true;
    }

    // 다른 서버가 점유한 Outbox를 변경하지 못하게 합니다.
    private boolean isOwnedBy(String lockOwner) {
        return this.publishStatus == VitamateCleanupOutbox.PublishStatus.PENDING
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

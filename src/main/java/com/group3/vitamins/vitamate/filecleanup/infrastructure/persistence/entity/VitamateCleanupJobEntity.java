package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;

import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupJob;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;

// ChromaDB 파생 데이터 정리 작업의 처리 상태를 저장합니다.
@Getter
@Entity
@Table(name = "vitamate_cleanup_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitamateCleanupJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cleanup_job_id")
    private Long cleanupJobId;

    @Column(
            name = "cleanup_key",
            nullable = false,
            length = 36,
            columnDefinition = "CHAR(36)",
            updatable = false
    )
    private String cleanupKey;

    @Column(name = "file_id", nullable = false, updatable = false)
    private Long fileId;

    @Column(name = "file_version_ids", nullable = false, columnDefinition = "JSON", updatable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode fileVersionIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleanup_status", nullable = false, length = 20)
    private VitamateCleanupJob.Status cleanupStatus;

    @Column(
            name = "current_attempt_id",
            length = 36,
            columnDefinition = "CHAR(36)"
    )
    private String currentAttemptId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "deleted_vector_count", nullable = false)
    private int deletedVectorCount;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 현재 처리 시도가 없으면 새로운 attempt를 발급합니다.
    public void prepareAttempt(String attemptId, LocalDateTime now) {
        if (this.currentAttemptId != null) {
            return;
        }

        this.currentAttemptId = attemptId;
        this.attemptCount += 1;
        this.updatedAt = now;
    }

    // Redis 발행 완료 후 작업을 PUBLISHED로 변경합니다.
    public boolean markPublished(String attemptId, LocalDateTime now) {
        if (attemptId == null || !Objects.equals(attemptId, this.currentAttemptId)) {
            return false;
        }

        this.cleanupStatus = VitamateCleanupJob.Status.PUBLISHED;
        this.updatedAt = now;
        return true;
    }

    // 파일 삭제 트랜잭션에서 최초 대기 상태의 정리 작업을 생성합니다.
    public static VitamateCleanupJobEntity waiting(
            String cleanupKey,
            Long fileId,
            JsonNode fileVersionIds,
            LocalDateTime now
    ) {
        VitamateCleanupJobEntity entity = new VitamateCleanupJobEntity();
        entity.cleanupKey = cleanupKey;
        entity.fileId = fileId;
        entity.fileVersionIds = fileVersionIds;
        entity.cleanupStatus = VitamateCleanupJob.Status.WAITING;
        entity.attemptCount = 0;
        entity.deletedVectorCount = 0;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }
}

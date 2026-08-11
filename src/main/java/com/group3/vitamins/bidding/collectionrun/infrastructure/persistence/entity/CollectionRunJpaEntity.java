package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionConditionJpaEntity;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
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
@Table(name = "crawl_run")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionRunJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crawl_run_id")
    private Long crawlRunId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crawl_condition_id", nullable = false)
    private CollectionConditionJpaEntity crawlCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private CollectionRunTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_status", nullable = false, length = 20)
    private CollectionRunStatus runStatus;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "collected_count", nullable = false)
    private int collectedCount;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "requested_by", length = 20)
    private String requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_snapshot", nullable = false, columnDefinition = "JSON")
    private JsonNode conditionSnapshot;

    @Column(
            name = "processing_attempt_id",
            columnDefinition = "CHAR(36)"
    )
    private String processingAttemptId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 필드 이름이 드러나는 입력 모델로 Entity를 구성해 위치 기반 매핑 오류를 막습니다.
    public static CollectionRunJpaEntity from(PersistenceValues values) {
        CollectionRunJpaEntity entity = new CollectionRunJpaEntity();
        entity.crawlRunId = values.crawlRunId();
        entity.crawlCondition = values.crawlCondition();
        entity.conditionSnapshot = values.conditionSnapshot();
        entity.triggerType = values.triggerType();
        entity.runStatus = values.runStatus();
        entity.processingAttemptId = values.processingAttemptId();
        entity.retryCount = values.retryCount();
        entity.processingStartedAt = values.processingStartedAt();
        entity.leaseExpiresAt = values.leaseExpiresAt();
        entity.startedAt = values.startedAt();
        entity.finishedAt = values.finishedAt();
        entity.collectedCount = values.collectedCount();
        entity.insertedCount = values.insertedCount();
        entity.updatedCount = values.updatedCount();
        entity.skippedCount = values.skippedCount();
        entity.errorCode = values.errorCode();
        entity.errorMessage = values.errorMessage();
        entity.requestedBy = values.requestedBy();
        entity.createdAt = values.createdAt();
        entity.updatedAt = values.updatedAt();
        entity.deletedAt = values.deletedAt();
        return entity;
    }

    public record PersistenceValues(
            Long crawlRunId,
            CollectionConditionJpaEntity crawlCondition,
            JsonNode conditionSnapshot,
            CollectionRunTriggerType triggerType,
            CollectionRunStatus runStatus,
            String processingAttemptId,
            int retryCount,
            LocalDateTime processingStartedAt,
            LocalDateTime leaseExpiresAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            int collectedCount,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            String errorCode,
            String errorMessage,
            String requestedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
    }
}

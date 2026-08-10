package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity;

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

    // 수집 실행 도메인 모델을 DB에 저장 가능한 Entity로 구성합니다.
    public CollectionRunJpaEntity(
            Long crawlRunId,
            CollectionConditionJpaEntity crawlCondition,
            CollectionRunTriggerType triggerType,
            CollectionRunStatus runStatus,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            int collectedCount,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            String errorMessage,
            String requestedBy,
            LocalDateTime createdAt,
            LocalDateTime deletedAt
    ) {
        this.crawlRunId = crawlRunId;
        this.crawlCondition = crawlCondition;
        this.triggerType = triggerType;
        this.runStatus = runStatus;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.collectedCount = collectedCount;
        this.insertedCount = insertedCount;
        this.updatedCount = updatedCount;
        this.skippedCount = skippedCount;
        this.errorMessage = errorMessage;
        this.requestedBy = requestedBy;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }
}
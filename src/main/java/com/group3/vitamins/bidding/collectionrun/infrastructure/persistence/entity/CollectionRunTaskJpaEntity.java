package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTaskStatus;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "crawl_run_task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionRunTaskJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crawl_run_task_id")
    private Long crawlRunTaskId;

    @Column(name = "crawl_run_id", nullable = false)
    private Long crawlRunId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_type", nullable = false, length = 30)
    private BidNoticeType noticeType;

    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;

    @Column(name = "region_code", nullable = false, length = 50)
    private String regionCode;

    @Column(name = "industry_code", nullable = false, length = 50)
    private String industryCode;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false, length = 20)
    private CollectionRunTaskStatus taskStatus;

    @Column(name = "attempt_id", columnDefinition = "CHAR(36)")
    private String attemptId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Column(name = "collected_count", nullable = false)
    private int collectedCount;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // 수집 실행과 외부 요청 조합을 새 task 엔티티로 변환합니다.
    public static CollectionRunTaskJpaEntity create(
            Long runId,
            CollectionRequestCombination target,
            LocalDateTime now
    ) {
        CollectionRunTaskJpaEntity entity = new CollectionRunTaskJpaEntity();
        entity.crawlRunId = runId;
        entity.noticeType = target.noticeType();
        entity.keyword = normalize(target.keyword());
        entity.regionCode = normalize(target.regionCode());
        entity.industryCode = normalize(target.industryCode());
        entity.pageNumber = target.pageNumber();
        entity.taskStatus = CollectionRunTaskStatus.PENDING;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}

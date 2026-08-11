package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "crawl_condition")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionConditionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crawl_condition_id")
    private Long crawlConditionId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crawl_source_id", nullable = false)
    private CollectionSourceJpaEntity crawlSource;

    @Column(name = "condition_name", nullable = false, length = 100)
    private String conditionName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "JSON")
    private JsonNode params;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "auto_collection_enabled", nullable = false)
    private boolean autoCollectionEnabled;

    @Column(name = "schedule_type", length = 20)
    private String scheduleType;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "last_scheduled_at")
    private LocalDateTime lastScheduledAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_collected_count")
    private Integer lastCollectedCount;

    @Column(name = "created_by", length = 20, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 도메인 객체를 DB에 저장할 수 있는 JPA Entity로 구성합니다.
    public CollectionConditionJpaEntity(
            Long crawlConditionId,
            Long companyId,
            CollectionSourceJpaEntity crawlSource,
            String conditionName,
            JsonNode params,
            boolean enabled,
            boolean autoCollectionEnabled,
            String scheduleType,
            LocalTime scheduledTime,
            String timezone,
            LocalDateTime nextRunAt,
            LocalDateTime lastScheduledAt,
            LocalDateTime lastSuccessAt,
            Integer lastCollectedCount,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this.crawlConditionId = crawlConditionId;
        this.companyId = companyId;
        this.crawlSource = crawlSource;
        this.conditionName = conditionName;
        this.params = params;
        this.enabled = enabled;
        this.autoCollectionEnabled = autoCollectionEnabled;
        this.scheduleType = scheduleType;
        this.scheduledTime = scheduledTime;
        this.timezone = timezone;
        this.nextRunAt = nextRunAt;
        this.lastScheduledAt = lastScheduledAt;
        this.lastSuccessAt = lastSuccessAt;
        this.lastCollectedCount = lastCollectedCount;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public CollectionConditionJpaEntity(
            Long crawlConditionId, Long companyId,
            CollectionSourceJpaEntity crawlSource, String conditionName,
            JsonNode params, boolean enabled, LocalDateTime lastSuccessAt,
            Integer lastCollectedCount, String createdBy,
            LocalDateTime createdAt, LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this(crawlConditionId, companyId, crawlSource, conditionName, params,
                enabled, false, null, null, null, null, null,
                lastSuccessAt, lastCollectedCount, createdBy, createdAt,
                updatedAt, deletedAt);
    }
}

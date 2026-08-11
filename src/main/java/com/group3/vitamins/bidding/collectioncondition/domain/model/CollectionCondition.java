package com.group3.vitamins.bidding.collectioncondition.domain.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class CollectionCondition {

    private final Long conditionId;
    private final Long companyId;
    private final String sourceCode;
    private String conditionName;
    private List<BidNoticeType> noticeTypes;
    private CollectionConditionFilter filters;
    private boolean active;
    private boolean autoCollectionEnabled;
    private CollectionScheduleType scheduleType;
    private LocalTime scheduledTime;
    private String timezone;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastScheduledAt;
    private LocalDateTime lastSuccessAt;
    private Integer lastCollectedCount;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private CollectionCondition(
            Long conditionId,
            Long companyId,
            String sourceCode,
            String conditionName,
            List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters,
            boolean active,
            boolean autoCollectionEnabled,
            CollectionScheduleType scheduleType,
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
        this.conditionId = conditionId;
        this.companyId = companyId;
        this.sourceCode = sourceCode;
        this.conditionName = conditionName;
        this.noticeTypes = List.copyOf(noticeTypes);
        this.filters = filters;
        this.active = active;
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

    /// 현재 로그인한 사용자의 회사 소속으로 수집 조건을 생성합니다.
    public static CollectionCondition create(
            Long companyId,
            String sourceCode,
            String conditionName,
            List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters,
            boolean active,
            boolean autoCollectionEnabled,
            CollectionScheduleType scheduleType,
            LocalTime scheduledTime,
            String timezone,
            LocalDateTime nextRunAt,
            String createdBy,
            LocalDateTime now
    ) {
        return new CollectionCondition(
                null,
                companyId,
                sourceCode,
                conditionName,
                noticeTypes,
                filters,
                active,
                autoCollectionEnabled,
                scheduleType,
                scheduledTime,
                timezone,
                nextRunAt,
                null,
                null,
                null,
                createdBy,
                now,
                null,
                null
        );
    }

    public static CollectionCondition create(
            Long companyId, String sourceCode, String conditionName,
            List<BidNoticeType> noticeTypes, CollectionConditionFilter filters,
            boolean active, String createdBy, LocalDateTime now
    ) {
        return create(companyId, sourceCode, conditionName, noticeTypes, filters,
                active, false, null, null, null, null, createdBy, now);
    }

    // DB에서 조회한 회사별 수집 조건을 도메인 객체로 복원합니다.
    public static CollectionCondition restore(
            Long conditionId,
            Long companyId,
            String sourceCode,
            String conditionName,
            List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters,
            boolean active,
            boolean autoCollectionEnabled,
            CollectionScheduleType scheduleType,
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
        return new CollectionCondition(
                conditionId,
                companyId,
                sourceCode,
                conditionName,
                noticeTypes,
                filters,
                active,
                autoCollectionEnabled,
                scheduleType,
                scheduledTime,
                timezone,
                nextRunAt,
                lastScheduledAt,
                lastSuccessAt,
                lastCollectedCount,
                createdBy,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public static CollectionCondition restore(
            Long conditionId, Long companyId, String sourceCode,
            String conditionName, List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters, boolean active,
            LocalDateTime lastSuccessAt, Integer lastCollectedCount,
            String createdBy, LocalDateTime createdAt,
            LocalDateTime updatedAt, LocalDateTime deletedAt
    ) {
        return restore(conditionId, companyId, sourceCode, conditionName,
                noticeTypes, filters, active, false, null, null, null,
                null, null, lastSuccessAt, lastCollectedCount, createdBy,
                createdAt, updatedAt, deletedAt);
    }

    // 수정 요청으로 전달된 수집 조건 내용을 전체 교체합니다.
    public void update(
            String conditionName,
            List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters,
            boolean active,
            boolean autoCollectionEnabled,
            CollectionScheduleType scheduleType,
            LocalTime scheduledTime,
            String timezone,
            LocalDateTime nextRunAt,
            LocalDateTime updatedAt
    ) {
        this.conditionName = conditionName;
        this.noticeTypes = List.copyOf(noticeTypes);
        this.filters = filters;
        this.active = active;
        this.autoCollectionEnabled = autoCollectionEnabled;
        this.scheduleType = scheduleType;
        this.scheduledTime = scheduledTime;
        this.timezone = timezone;
        this.nextRunAt = nextRunAt;
        this.updatedAt = updatedAt;
    }

    public void update(
            String conditionName, List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters, boolean active,
            LocalDateTime updatedAt
    ) {
        update(conditionName, noticeTypes, filters, active,
                false, null, null, null, null, updatedAt);
    }

    // 성공한 수집 실행의 시각과 수집 건수를 기록합니다.
    public void recordCollectionSuccess(
            LocalDateTime lastSuccessAt,
            int lastCollectedCount,
            LocalDateTime updatedAt
    ) {
        this.lastSuccessAt = lastSuccessAt;
        this.lastCollectedCount = lastCollectedCount;
        this.updatedAt = updatedAt;
    }

    // 자동 실행이 생성된 예약 시각과 다음 실행 시각을 기록합니다.
    public void recordScheduledRun(
            LocalDateTime scheduledAt,
            LocalDateTime nextRunAt,
            LocalDateTime updatedAt
    ) {
        this.lastScheduledAt = scheduledAt;
        this.nextRunAt = nextRunAt;
        this.updatedAt = updatedAt;
    }

    // 진행 중인 실행이 있어 건너뛴 예약의 다음 실행 시각만 갱신합니다.
    public void advanceSchedule(
            LocalDateTime nextRunAt,
            LocalDateTime updatedAt
    ) {
        this.nextRunAt = nextRunAt;
        this.updatedAt = updatedAt;
    }

    // 수집 조건을 논리 삭제합니다.
    public void delete(LocalDateTime deletedAt) {
        this.active = false;
        this.deletedAt = deletedAt;
        this.updatedAt = deletedAt;
    }

    // 논리 삭제 여부를 반환합니다.
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Long getConditionId() {
        return conditionId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getConditionName() {
        return conditionName;
    }

    public List<BidNoticeType> getNoticeTypes() {
        return noticeTypes;
    }

    public CollectionConditionFilter getFilters() {
        return filters;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAutoCollectionEnabled() { return autoCollectionEnabled; }
    public CollectionScheduleType getScheduleType() { return scheduleType; }
    public LocalTime getScheduledTime() { return scheduledTime; }
    public String getTimezone() { return timezone; }
    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public LocalDateTime getLastScheduledAt() { return lastScheduledAt; }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public Integer getLastCollectedCount() {
        return lastCollectedCount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}

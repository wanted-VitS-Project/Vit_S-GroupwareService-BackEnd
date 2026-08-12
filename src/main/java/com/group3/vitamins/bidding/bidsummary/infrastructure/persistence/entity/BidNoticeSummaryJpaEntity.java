package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummary;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "bid_notice_summary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidNoticeSummaryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_notice_summary_id")
    private Long summaryId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "bid_notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "parent_summary_id")
    private Long parentSummaryId;

    @Column(name = "revision_no", nullable = false)
    private int revisionNo;

    @Column(name = "requested_by", nullable = false, length = 20)
    private String requestedBy;

    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notice_snapshot", nullable = false, columnDefinition = "JSON")
    private JsonNode noticeSnapshot;

    @Column(name = "overview_summary", columnDefinition = "TEXT")
    private String overviewSummary;

    @Column(name = "amount_summary", columnDefinition = "TEXT")
    private String amountSummary;

    @Column(name = "schedule_summary", columnDefinition = "TEXT")
    private String scheduleSummary;

    @Column(name = "qualification_summary", columnDefinition = "TEXT")
    private String qualificationSummary;

    @Column(name = "task_summary", columnDefinition = "TEXT")
    private String taskSummary;

    @Column(name = "risk_summary", columnDefinition = "TEXT")
    private String riskSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", nullable = false, length = 20)
    private BidNoticeSummaryStatus summaryStatus;

    @Column(
            name = "processing_attempt_id",
            nullable = false,
            length = 36,
            columnDefinition = "CHAR(36)"
    )
    private String processingAttemptId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "confirmed", nullable = false)
    private boolean confirmed;

    @Column(name = "confirmed_by", length = 20)
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // 최초 AI 요약 요청을 PENDING 상태로 저장합니다.
    public static BidNoticeSummaryJpaEntity pending(
            BidNoticeSummary summary,
            JsonNode noticeSnapshot
    ) {
        BidNoticeSummaryJpaEntity entity = new BidNoticeSummaryJpaEntity();

        entity.companyId = summary.companyId();
        entity.noticeId = summary.noticeId();
        entity.parentSummaryId = summary.parentSummaryId();
        entity.revisionNo = summary.revisionNo();
        entity.requestedBy = summary.requestedBy();
        entity.prompt = summary.prompt();
        entity.noticeSnapshot = noticeSnapshot;
        entity.summaryStatus = summary.summaryStatus();
        entity.processingAttemptId = summary.processingAttemptId();
        entity.retryCount = summary.retryCount();
        entity.confirmed = false;
        entity.createdAt = summary.createdAt();
        entity.updatedAt = summary.createdAt();

        return entity;
    }

    public BidNoticeSummary toDomain() {
        return new BidNoticeSummary(
                summaryId,
                companyId,
                noticeId,
                parentSummaryId,
                revisionNo,
                requestedBy,
                prompt,
                summaryStatus,
                processingAttemptId,
                retryCount,
                createdAt
        );
    }

    // 현재 worker가 이 요약 작업을 조회할 수 있는지 확인합니다.
    public boolean canClaim(String attemptId) {
        return processingAttemptId.equals(attemptId)
                && (summaryStatus == BidNoticeSummaryStatus.PENDING
                || summaryStatus == BidNoticeSummaryStatus.PROCESSING);
    }

    // 최초 작업 조회 시 PROCESSING으로 전환합니다.
    public void startProcessing(LocalDateTime now) {
        if (summaryStatus == BidNoticeSummaryStatus.PENDING) {
            summaryStatus = BidNoticeSummaryStatus.PROCESSING;
            processingStartedAt = now;
            updatedAt = now;
        }
    }

    // 현재 처리 중인 attempt인지 확인합니다.
    public boolean isCurrentProcessingAttempt(String attemptId) {
        return summaryStatus == BidNoticeSummaryStatus.PROCESSING
                && processingAttemptId.equals(attemptId);
    }

    // AI 요약 결과를 완료 상태로 저장합니다.
    public void complete(
            String overviewSummary,
            String amountSummary,
            String scheduleSummary,
            String qualificationSummary,
            String taskSummary,
            String riskSummary,
            LocalDateTime now
    ) {
        this.overviewSummary = overviewSummary;
        this.amountSummary = amountSummary;
        this.scheduleSummary = scheduleSummary;
        this.qualificationSummary = qualificationSummary;
        this.taskSummary = taskSummary;
        this.riskSummary = riskSummary;
        this.errorMessage = null;
        this.summaryStatus = BidNoticeSummaryStatus.COMPLETED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    // AI 요약 작업을 실패 상태로 저장합니다.
    public void fail(String errorMessage, LocalDateTime now) {
        this.overviewSummary = null;
        this.amountSummary = null;
        this.scheduleSummary = null;
        this.qualificationSummary = null;
        this.taskSummary = null;
        this.riskSummary = null;
        this.errorMessage = errorMessage;
        this.summaryStatus = BidNoticeSummaryStatus.FAILED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public BidNoticeSummaryDetails toDetails() {
        return new BidNoticeSummaryDetails(
                summaryId, companyId, noticeId, parentSummaryId, revisionNo,
                requestedBy, prompt,
                summaryStatus, overviewSummary, amountSummary,
                scheduleSummary, qualificationSummary, taskSummary,
                riskSummary, confirmed, confirmedBy, confirmedAt,
                projectId, errorMessage, createdAt, completedAt, updatedAt
        );
    }

    // 사용자가 검토한 구조화 요약값으로 전체 결과 필드를 갱신합니다.
    public void updateSummaries(
            String overviewSummary,
            String amountSummary,
            String scheduleSummary,
            String qualificationSummary,
            String taskSummary,
            String riskSummary,
            LocalDateTime now
    ) {
        this.overviewSummary = overviewSummary;
        this.amountSummary = amountSummary;
        this.scheduleSummary = scheduleSummary;
        this.qualificationSummary = qualificationSummary;
        this.taskSummary = taskSummary;
        this.riskSummary = riskSummary;
        this.updatedAt = now;
    }

    // 검토가 끝난 완료 요약을 요청자 명의로 확정합니다.
    public void confirm(String userId, LocalDateTime now) {
        this.confirmed = true;
        this.confirmedBy = userId;
        this.confirmedAt = now;
        this.updatedAt = now;
    }

    // 일시 장애 작업을 새 attemptId의 대기 상태로 전환합니다.
    public void prepareRetry(
            String nextAttemptId,
            String errorMessage,
            LocalDateTime now
    ) {
        this.overviewSummary = null;
        this.amountSummary = null;
        this.scheduleSummary = null;
        this.qualificationSummary = null;
        this.taskSummary = null;
        this.riskSummary = null;
        this.processingAttemptId = nextAttemptId;
        this.retryCount += 1;
        this.processingStartedAt = null;
        this.errorMessage = errorMessage;
        this.summaryStatus = BidNoticeSummaryStatus.PENDING;
        this.completedAt = null;
        this.updatedAt = now;
    }
}

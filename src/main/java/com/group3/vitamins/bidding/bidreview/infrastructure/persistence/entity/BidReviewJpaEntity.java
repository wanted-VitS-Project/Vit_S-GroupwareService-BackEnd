package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "bid_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidReviewJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_review_id")
    private Long reviewId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "bid_notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "requested_by", nullable = false, length = 20)
    private String requestedBy;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private BidReviewStatus reviewStatus;

    @Column(name = "processing_attempt_id", nullable = false,
            length = 36, columnDefinition = "CHAR(36)")
    private String processingAttemptId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "result", columnDefinition = "LONGTEXT")
    private String result;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "abandoned_at")
    private LocalDateTime abandonedAt;

    @Column(name = "cleanup_started_at")
    private LocalDateTime cleanupStartedAt;

    @Column(name = "cleanup_completed_at")
    private LocalDateTime cleanupCompletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static BidReviewJpaEntity from(BidReview review) {
        BidReviewJpaEntity entity = new BidReviewJpaEntity();
        entity.reviewId = review.reviewId();
        entity.apply(review);
        return entity;
    }

    public void apply(BidReview review) {
        this.companyId = review.companyId();
        this.noticeId = review.noticeId();
        this.requestedBy = review.requestedBy();
        this.projectId = review.projectId();
        this.prompt = review.prompt();
        this.reviewStatus = review.reviewStatus();
        this.processingAttemptId = review.processingAttemptId();
        this.retryCount = review.retryCount();
        this.result = review.result();
        this.errorCode = review.errorCode();
        this.errorMessage = review.errorMessage();
        this.completedAt = review.completedAt();
        this.expiresAt = review.expiresAt();
        this.abandonedAt = review.abandonedAt();
        this.cleanupStartedAt = review.cleanupStartedAt();
        this.cleanupCompletedAt = review.cleanupCompletedAt();
        this.createdAt = review.createdAt();
        this.updatedAt = review.updatedAt();
    }

    public BidReview toDomain() {
        return new BidReview(
                reviewId,
                companyId,
                noticeId,
                requestedBy,
                projectId,
                prompt,
                reviewStatus,
                processingAttemptId,
                retryCount,
                result,
                errorCode,
                errorMessage,
                completedAt,
                expiresAt,
                abandonedAt,
                cleanupStartedAt,
                cleanupCompletedAt,
                createdAt,
                updatedAt
        );
    }
}

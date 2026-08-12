package com.group3.vitamins.bidding.bidreview.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record BidReview(
        Long reviewId,
        Long companyId,
        Long noticeId,
        String requestedBy,
        Long projectId,
        String prompt,
        BidReviewStatus reviewStatus,
        String processingAttemptId,
        int retryCount,
        String result,
        String errorCode,
        String errorMessage,
        LocalDateTime completedAt,
        LocalDateTime expiresAt,
        LocalDateTime abandonedAt,
        LocalDateTime cleanupStartedAt,
        LocalDateTime cleanupCompletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // 새 검토를 Worker 처리 대기 상태로 생성합니다.
    public static BidReview createPending(
            Long companyId,
            Long noticeId,
            String requestedBy,
            String prompt,
            String attemptId,
            LocalDateTime now
    ) {
        Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
        Objects.requireNonNull(noticeId, "입찰 공고 ID는 필수입니다.");
        Objects.requireNonNull(requestedBy, "요청자 ID는 필수입니다.");
        Objects.requireNonNull(prompt, "검토 프롬프트는 필수입니다.");
        Objects.requireNonNull(attemptId, "처리 시도 ID는 필수입니다.");
        Objects.requireNonNull(now, "생성 시각은 필수입니다.");

        if (prompt.isBlank() || prompt.length() > 3000) {
            throw new IllegalArgumentException("검토 프롬프트가 올바르지 않습니다.");
        }

        return new BidReview(
                null,
                companyId,
                noticeId,
                requestedBy,
                null,
                prompt,
                BidReviewStatus.PENDING,
                attemptId,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    // Worker가 검토 처리를 시작합니다.
    public BidReview startProcessing(LocalDateTime now) {
        requireWorkerCallbackState();
        Objects.requireNonNull(now, "처리 시작 시각은 필수입니다.");

        return copy(
                projectId,
                BidReviewStatus.PROCESSING,
                processingAttemptId,
                retryCount,
                null,
                null,
                null,
                null,
                null,
                abandonedAt,
                cleanupStartedAt,
                cleanupCompletedAt,
                now
        );
    }

    // AI 검토 결과를 완료 상태로 저장하고 임시파일 만료 시각을 설정합니다.
    public BidReview complete(
            String result,
            LocalDateTime completedAt
    ) {
        requireWorkerCallbackState();
        Objects.requireNonNull(result, "검토 결과는 필수입니다.");
        Objects.requireNonNull(completedAt, "완료 시각은 필수입니다.");

        if (result.isBlank()) {
            throw new IllegalArgumentException("검토 결과는 비어 있을 수 없습니다.");
        }

        return copy(
                projectId,
                BidReviewStatus.COMPLETED,
                processingAttemptId,
                retryCount,
                result,
                null,
                null,
                completedAt,
                completedAt.plusHours(3),
                abandonedAt,
                cleanupStartedAt,
                cleanupCompletedAt,
                completedAt
        );
    }

    // 오류 정보를 저장하고 임시파일 만료 시각을 설정합니다.
    public BidReview fail(
            String errorCode,
            String errorMessage,
            LocalDateTime failedAt
    ) {
        requireWorkerCallbackState();
        Objects.requireNonNull(errorMessage, "실패 메시지는 필수입니다.");
        Objects.requireNonNull(failedAt, "실패 시각은 필수입니다.");

        if (errorMessage.isBlank() || errorMessage.length() > 500) {
            throw new IllegalArgumentException("실패 메시지가 올바르지 않습니다.");
        }

        return copy(
                projectId,
                BidReviewStatus.FAILED,
                processingAttemptId,
                retryCount,
                null,
                errorCode,
                errorMessage,
                failedAt,
                failedAt.plusHours(3),
                abandonedAt,
                cleanupStartedAt,
                cleanupCompletedAt,
                failedAt
        );
    }

    // 프로젝트로 전환하지 않은 검토를 포기합니다.
    public BidReview abandon(LocalDateTime now) {
        Objects.requireNonNull(now, "포기 시각은 필수입니다.");

        if (projectId != null || !reviewStatus.isAbandonable()) {
            throw new IllegalStateException("현재 검토는 포기할 수 없습니다.");
        }

        return copy(
                null,
                BidReviewStatus.ABANDONED,
                processingAttemptId,
                retryCount,
                result,
                errorCode,
                errorMessage,
                completedAt,
                now,
                now,
                cleanupStartedAt,
                cleanupCompletedAt,
                now
        );
    }

    // 정리가 끝난 미귀속 검토를 만료 상태로 전환합니다.
    public BidReview expire(LocalDateTime now) {
        Objects.requireNonNull(now, "정리 완료 시각은 필수입니다.");

        if (projectId != null
                || (reviewStatus != BidReviewStatus.COMPLETED
                && reviewStatus != BidReviewStatus.FAILED
                && reviewStatus != BidReviewStatus.ABANDONED)) {
            throw new IllegalStateException("현재 검토는 만료 처리할 수 없습니다.");
        }

        return copy(
                null,
                BidReviewStatus.EXPIRED,
                processingAttemptId,
                retryCount,
                result,
                errorCode,
                errorMessage,
                completedAt,
                expiresAt,
                abandonedAt,
                cleanupStartedAt,
                now,
                now
        );
    }

    public boolean matchesAttempt(String attemptId) {
        return Objects.equals(processingAttemptId, attemptId);
    }

    private void requireWorkerCallbackState() {
        if (!reviewStatus.acceptsWorkerCallback()) {
            throw new IllegalStateException("현재 검토 상태에서는 Worker 결과를 반영할 수 없습니다.");
        }
    }

    private BidReview copy(
            Long projectId,
            BidReviewStatus status,
            String attemptId,
            int retryCount,
            String result,
            String errorCode,
            String errorMessage,
            LocalDateTime completedAt,
            LocalDateTime expiresAt,
            LocalDateTime abandonedAt,
            LocalDateTime cleanupStartedAt,
            LocalDateTime cleanupCompletedAt,
            LocalDateTime updatedAt
    ) {
        return new BidReview(
                reviewId,
                companyId,
                noticeId,
                requestedBy,
                projectId,
                prompt,
                status,
                attemptId,
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
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

        if (prompt.isBlank()
                || prompt.codePointCount(0, prompt.length()) > 3000) {
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

    // AI 검토 결과를 완료 상태로 저장하고 임시파일 만료 시각을 설정합니다. 공고 입찰마감일시까지
    // 보관하고(2026-08-13 정책 변경 - 종전 완료 후 고정 3시간), 마감일이 없거나 이미 지났으면
    // 완료 시각 + 3시간으로 되돌린다(fallback).
    public BidReview complete(
            String result,
            LocalDateTime completedAt,
            LocalDateTime noticeDeadlineAt
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
                resolveExpiresAt(completedAt, noticeDeadlineAt),
                abandonedAt,
                cleanupStartedAt,
                cleanupCompletedAt,
                completedAt
        );
    }

    // 오류 정보를 저장하고 임시파일 만료 시각을 설정합니다. complete()와 동일한 보관 정책을 따른다.
    public BidReview fail(
            String errorCode,
            String errorMessage,
            LocalDateTime failedAt,
            LocalDateTime noticeDeadlineAt
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
                resolveExpiresAt(failedAt, noticeDeadlineAt),
                abandonedAt,
                cleanupStartedAt,
                cleanupCompletedAt,
                failedAt
        );
    }

    // 공고 입찰마감일시까지 보관한다 - 마감일이 없거나(NULL) 이미 지난 과거 시각이면 판단 기준으로 쓸 수
    // 없으므로 기존 정책(종료 시각 + 3시간)으로 되돌린다.
    private static LocalDateTime resolveExpiresAt(LocalDateTime baseAt, LocalDateTime noticeDeadlineAt) {
        if (noticeDeadlineAt != null && noticeDeadlineAt.isAfter(baseAt)) {
            return noticeDeadlineAt;
        }
        return baseAt.plusHours(3);
    }

    // Worker의 처리 실패가 일시적이면 새 attemptId를 발급해 재시도 대기 상태로 되돌립니다.
    // 최대 재시도 횟수 판단은 호출자(JpaBidReviewWorkerAdapter)가 retryCount()를 보고 먼저 한다 —
    // 이 메서드는 "재시도한다"는 결정이 내려진 뒤의 상태 전이만 책임진다.
    public BidReview retryProcessing(
            String nextAttemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime now
    ) {
        requireWorkerCallbackState();
        Objects.requireNonNull(nextAttemptId, "재시도 처리 시도 ID는 필수입니다.");
        Objects.requireNonNull(now, "재시도 시각은 필수입니다.");

        return copy(
                projectId,
                BidReviewStatus.PENDING,
                nextAttemptId,
                retryCount + 1,
                null,
                errorCode,
                errorMessage,
                null,
                null,
                abandonedAt,
                cleanupStartedAt,
                cleanupCompletedAt,
                now
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
                && reviewStatus != BidReviewStatus.ABANDONED)
                || expiresAt == null
                || now.isBefore(expiresAt)) {
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

    // 정리 스캔이 이 검토를 점유했음을 표시합니다(중복 정리 요청 방지). 상태는 바꾸지 않습니다.
    public BidReview markCleanupStarted(LocalDateTime now) {
        Objects.requireNonNull(now, "정리 시작 시각은 필수입니다.");
        return copy(
                projectId,
                reviewStatus,
                processingAttemptId,
                retryCount,
                result,
                errorCode,
                errorMessage,
                completedAt,
                expiresAt,
                abandonedAt,
                now,
                cleanupCompletedAt,
                now
        );
    }
}
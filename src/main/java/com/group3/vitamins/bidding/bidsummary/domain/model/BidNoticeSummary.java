package com.group3.vitamins.bidding.bidsummary.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record BidNoticeSummary(
        Long summaryId,
        Long companyId,
        Long noticeId,
        String requestedBy,
        String prompt,
        BidNoticeSummaryStatus summaryStatus,
        String processingAttemptId,
        int retryCount,
        LocalDateTime createdAt
) {

    // 새로운 AI 요약 요청을 대기 상태로 생성합니다.
    public static BidNoticeSummary createPending(
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
        Objects.requireNonNull(prompt, "프롬프트는 필수입니다.");
        Objects.requireNonNull(attemptId, "처리 시도 ID는 필수입니다.");
        Objects.requireNonNull(now, "생성 시각은 필수입니다.");

        return new BidNoticeSummary(
                null,
                companyId,
                noticeId,
                requestedBy,
                prompt,
                BidNoticeSummaryStatus.PENDING,
                attemptId,
                0,
                now
        );
    }
}
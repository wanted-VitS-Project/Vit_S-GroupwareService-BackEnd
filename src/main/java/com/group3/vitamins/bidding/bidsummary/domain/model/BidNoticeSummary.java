package com.group3.vitamins.bidding.bidsummary.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record BidNoticeSummary(
        Long summaryId,
        Long companyId,
        Long noticeId,
        Long parentSummaryId,
        int revisionNo,
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
        return createPending(
                companyId,
                noticeId,
                requestedBy,
                prompt,
                null,
                1,
                attemptId,
                now
        );
    }

    // 기존 완료 요약을 기준으로 새로운 개정 요약을 대기 상태로 생성합니다.
    public static BidNoticeSummary createPending(
            Long companyId,
            Long noticeId,
            String requestedBy,
            String prompt,
            Long parentSummaryId,
            int revisionNo,
            String attemptId,
            LocalDateTime now
    ) {
        Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
        Objects.requireNonNull(noticeId, "입찰 공고 ID는 필수입니다.");
        Objects.requireNonNull(requestedBy, "요청자 ID는 필수입니다.");
        Objects.requireNonNull(prompt, "프롬프트는 필수입니다.");
        Objects.requireNonNull(attemptId, "처리 시도 ID는 필수입니다.");
        Objects.requireNonNull(now, "생성 시각은 필수입니다.");
        if (revisionNo < 1 || (parentSummaryId == null && revisionNo != 1)) {
            throw new IllegalArgumentException("요약 개정 정보가 올바르지 않습니다.");
        }

        return new BidNoticeSummary(
                null,
                companyId,
                noticeId,
                parentSummaryId,
                revisionNo,
                requestedBy,
                prompt,
                BidNoticeSummaryStatus.PENDING,
                attemptId,
                0,
                now
        );
    }
}

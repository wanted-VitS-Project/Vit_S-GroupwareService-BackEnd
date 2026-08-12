package com.group3.vitamins.bidding.bidsummary.domain.model;

import java.time.LocalDateTime;

// 공개 조회와 사용자 검토에 필요한 입찰 AI 요약 전체 상태입니다.
public record BidNoticeSummaryDetails(
        Long summaryId,
        Long companyId,
        Long noticeId,
        String requestedBy,
        String prompt,
        BidNoticeSummaryStatus summaryStatus,
        String overviewSummary,
        String amountSummary,
        String scheduleSummary,
        String qualificationSummary,
        String taskSummary,
        String riskSummary,
        boolean confirmed,
        String confirmedBy,
        LocalDateTime confirmedAt,
        Long projectId,
        String errorMessage,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {
}

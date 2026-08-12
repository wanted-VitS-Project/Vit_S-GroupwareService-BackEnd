package com.group3.vitamins.bidding.bidsummary.application.result;

import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;

import java.time.LocalDateTime;

public record BidNoticeSummaryResult(
        Long summaryId,
        Long noticeId,
        String prompt,
        String summaryStatus,
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
    public static BidNoticeSummaryResult from(BidNoticeSummaryDetails details) {
        return new BidNoticeSummaryResult(
                details.summaryId(), details.noticeId(), details.prompt(),
                details.summaryStatus().name(), details.overviewSummary(),
                details.amountSummary(), details.scheduleSummary(),
                details.qualificationSummary(), details.taskSummary(),
                details.riskSummary(), details.confirmed(),
                details.confirmedBy(), details.confirmedAt(),
                details.projectId(), details.errorMessage(),
                details.requestedAt(), details.completedAt(), details.updatedAt()
        );
    }
}

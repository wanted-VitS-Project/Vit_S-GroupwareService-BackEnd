package com.group3.vitamins.bidding.bidsummary.application.result;

import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;

import java.time.LocalDateTime;

public record AbandonBidNoticeSummaryResult(
        Long summaryId,
        String summaryStatus,
        LocalDateTime abandonedAt
) {

    public static AbandonBidNoticeSummaryResult from(BidNoticeSummaryDetails details) {
        return new AbandonBidNoticeSummaryResult(
                details.summaryId(),
                details.summaryStatus().name(),
                details.completedAt()
        );
    }
}

package com.group3.vitamins.bidding.bidsummary.application.result;

import java.time.LocalDateTime;

public record CreateBidNoticeSummaryResult(
        Long summaryId,
        String summaryStatus,
        LocalDateTime requestedAt
) {
}
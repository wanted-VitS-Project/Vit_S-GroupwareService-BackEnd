package com.group3.vitamins.bidding.bidsummary.application.result;

import java.time.LocalDateTime;

public record BidNoticeSummaryHistoryItemResult(
        Long summaryId,
        Long parentSummaryId,
        int revisionNo,
        String summaryStatus,
        String prompt,
        boolean confirmed,
        boolean mine,
        Long projectId,
        LocalDateTime requestedAt,
        LocalDateTime confirmedAt
) {
}

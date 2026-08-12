package com.group3.vitamins.bidding.bidsummary.application.result;

public record BidNoticeSummaryCallbackResult(
        boolean accepted,
        Long summaryId,
        String summaryStatus,
        String reason
) {
}
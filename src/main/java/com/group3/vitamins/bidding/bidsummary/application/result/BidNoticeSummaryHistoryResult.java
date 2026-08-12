package com.group3.vitamins.bidding.bidsummary.application.result;

import java.util.List;

public record BidNoticeSummaryHistoryResult(
        Long latestMySummaryId,
        List<BidNoticeSummaryHistoryItemResult> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public BidNoticeSummaryHistoryResult {
        content = List.copyOf(content);
    }
}

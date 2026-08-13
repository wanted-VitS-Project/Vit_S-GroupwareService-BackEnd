package com.group3.vitamins.bidding.bidreview.application.result;

import java.time.LocalDateTime;
import java.util.List;

public record BidReviewHistoryResult(
        List<HistoryItemResult> content
) {

    public record HistoryItemResult(
            Long reviewId,
            String reviewStatus,
            String prompt,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            LocalDateTime expiresAt,
            Long projectId
    ) {
    }
}
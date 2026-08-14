package com.group3.vitamins.bidding.bidreview.application.port;

import java.time.LocalDateTime;
import java.util.List;

public interface BidReviewHistoryQueryPort {

    List<HistoryRow> findHistory(Long companyId, Long noticeId, String userId, int offset, int size);

    long countHistory(Long companyId, Long noticeId, String userId);

    record HistoryRow(
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
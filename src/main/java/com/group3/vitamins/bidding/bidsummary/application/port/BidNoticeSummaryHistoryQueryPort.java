package com.group3.vitamins.bidding.bidsummary.application.port;

import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryItemResult;

import java.util.List;

public interface BidNoticeSummaryHistoryQueryPort {

    List<BidNoticeSummaryHistoryItemResult> findHistory(
            Long companyId,
            Long noticeId,
            String userId,
            int offset,
            int size
    );

    long countHistory(Long companyId, Long noticeId, String userId);

    Long findLatestMineSummaryId(Long companyId, Long noticeId, String userId);
}

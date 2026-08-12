package com.group3.vitamins.bidding.bidsummary.infrastructure.query;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryHistoryQueryPort;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryItemResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MyBatisBidNoticeSummaryHistoryQueryAdapter
        implements BidNoticeSummaryHistoryQueryPort {

    private final BidNoticeSummaryHistoryQueryMapper mapper;

    @Override
    public List<BidNoticeSummaryHistoryItemResult> findHistory(
            Long companyId,
            Long noticeId,
            String userId,
            int offset,
            int size
    ) {
        return mapper.findHistory(companyId, noticeId, userId, offset, size);
    }

    @Override
    public long countHistory(Long companyId, Long noticeId, String userId) {
        return mapper.countHistory(companyId, noticeId, userId);
    }

    @Override
    public Long findLatestMineSummaryId(
            Long companyId,
            Long noticeId,
            String userId
    ) {
        return mapper.findLatestMineSummaryId(companyId, noticeId, userId);
    }
}

package com.group3.vitamins.bidding.bidsummary.application.result;

import com.group3.vitamins.bidding.bidsummary.application.port
        .BidNoticeSummaryNoticePort.BidNoticeSnapshot;

public record BidNoticeSummaryJobResult(
        Long summaryId,
        Long companyId,
        String attemptId,
        String prompt,
        BidNoticeSnapshot notice
) {
}
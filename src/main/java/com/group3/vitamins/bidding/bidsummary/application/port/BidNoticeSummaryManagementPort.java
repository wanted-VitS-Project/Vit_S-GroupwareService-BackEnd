package com.group3.vitamins.bidding.bidsummary.application.port;

import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BidNoticeSummaryManagementPort {

    Optional<BidNoticeSummaryDetails> findAccessible(
            Long companyId, Long summaryId, String userId
    );

    Optional<BidNoticeSummaryDetails> findOwnedForUpdate(
            Long companyId, Long summaryId, String userId
    );

    BidNoticeSummaryDetails updateSummaries(
            Long summaryId,
            SummaryValues values,
            LocalDateTime now
    );

    BidNoticeSummaryDetails confirm(
            Long summaryId,
            String confirmedBy,
            LocalDateTime now
    );

    // 진행 중(PENDING/PROCESSING)인 요약을 중단합니다. 진행 중이 아니면 IllegalStateException.
    BidNoticeSummaryDetails abandon(
            Long summaryId,
            LocalDateTime now
    );

    record SummaryValues(
            String overviewSummary,
            String amountSummary,
            String scheduleSummary,
            String qualificationSummary,
            String taskSummary,
            String riskSummary
    ) {
    }
}

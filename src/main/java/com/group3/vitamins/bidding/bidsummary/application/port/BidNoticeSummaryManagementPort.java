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

package com.group3.vitamins.bidding.bidsummary.application.result;

import java.time.LocalDateTime;

public record ConfirmBidNoticeSummaryResult(
        Long summaryId,
        boolean confirmed,
        String confirmedBy,
        LocalDateTime confirmedAt,
        boolean projectCreationAllowed
) {
}

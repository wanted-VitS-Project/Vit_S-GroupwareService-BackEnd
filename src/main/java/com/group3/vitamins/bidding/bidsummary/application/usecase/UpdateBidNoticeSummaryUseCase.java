package com.group3.vitamins.bidding.bidsummary.application.usecase;

import com.group3.vitamins.bidding.bidsummary.application.command.UpdateBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryResult;

public interface UpdateBidNoticeSummaryUseCase {
    BidNoticeSummaryResult update(UpdateBidNoticeSummaryCommand command);
}

package com.group3.vitamins.bidding.bidsummary.application.usecase;

import com.group3.vitamins.bidding.bidsummary.application.command.ConfirmBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.result.ConfirmBidNoticeSummaryResult;

public interface ConfirmBidNoticeSummaryUseCase {
    ConfirmBidNoticeSummaryResult confirm(ConfirmBidNoticeSummaryCommand command);
}

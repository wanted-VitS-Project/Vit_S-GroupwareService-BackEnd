package com.group3.vitamins.bidding.bidsummary.application.usecase;

import com.group3.vitamins.bidding.bidsummary.application.command.AbandonBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.result.AbandonBidNoticeSummaryResult;

public interface AbandonBidNoticeSummaryUseCase {

    AbandonBidNoticeSummaryResult abandon(AbandonBidNoticeSummaryCommand command);
}

package com.group3.vitamins.bidding.bidsummary.application.usecase;

import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryResult;

public interface GetBidNoticeSummaryUseCase {
    BidNoticeSummaryResult get(GetBidNoticeSummaryQuery query);
}

package com.group3.vitamins.bidding.bidsummary.application.usecase;

import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryHistoryQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryResult;

public interface GetBidNoticeSummaryHistoryUseCase {

    BidNoticeSummaryHistoryResult get(GetBidNoticeSummaryHistoryQuery query);
}

package com.group3.vitamins.bidding.bidreview.application.usecase;

import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewHistoryQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewHistoryResult;

public interface GetBidReviewHistoryUseCase {

    BidReviewHistoryResult get(GetBidReviewHistoryQuery query);
}
package com.group3.vitamins.bidding.bidreview.application.usecase;

import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewSourcesQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewSourcesResult;

public interface GetBidReviewSourcesUseCase {

    BidReviewSourcesResult get(GetBidReviewSourcesQuery query);
}
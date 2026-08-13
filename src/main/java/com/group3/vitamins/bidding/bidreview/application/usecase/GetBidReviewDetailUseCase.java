package com.group3.vitamins.bidding.bidreview.application.usecase;

import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewDetailQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewDetailResult;

public interface GetBidReviewDetailUseCase {

    BidReviewDetailResult get(GetBidReviewDetailQuery query);
}
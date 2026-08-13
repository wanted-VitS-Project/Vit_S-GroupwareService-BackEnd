package com.group3.vitamins.bidding.bidreview.application.usecase;

import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewJobQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewJobResult;

public interface GetBidReviewJobUseCase {

    BidReviewJobResult handle(GetBidReviewJobQuery query);
}

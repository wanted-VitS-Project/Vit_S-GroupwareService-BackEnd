package com.group3.vitamins.bidding.bidreview.application.usecase;

import com.group3.vitamins.bidding.bidreview.application.command.CreateBidReviewCommand;
import com.group3.vitamins.bidding.bidreview.application.result.CreateBidReviewResult;

public interface CreateBidReviewUseCase {

    CreateBidReviewResult create(CreateBidReviewCommand command);
}
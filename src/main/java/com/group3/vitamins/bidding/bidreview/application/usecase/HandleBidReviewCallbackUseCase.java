package com.group3.vitamins.bidding.bidreview.application.usecase;

import com.group3.vitamins.bidding.bidreview.application.command.HandleBidReviewCallbackCommand;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewCallbackResult;

public interface HandleBidReviewCallbackUseCase {

    BidReviewCallbackResult handle(HandleBidReviewCallbackCommand command);
}

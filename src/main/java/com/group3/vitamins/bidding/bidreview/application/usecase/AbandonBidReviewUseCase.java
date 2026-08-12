package com.group3.vitamins.bidding.bidreview.application.usecase;

import com.group3.vitamins.bidding.bidreview.application.command.AbandonBidReviewCommand;
import com.group3.vitamins.bidding.bidreview.application.result.AbandonBidReviewResult;

public interface AbandonBidReviewUseCase {

    AbandonBidReviewResult abandon(AbandonBidReviewCommand command);
}

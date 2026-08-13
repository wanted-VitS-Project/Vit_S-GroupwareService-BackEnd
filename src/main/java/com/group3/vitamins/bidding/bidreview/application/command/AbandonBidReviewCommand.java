package com.group3.vitamins.bidding.bidreview.application.command;

public record AbandonBidReviewCommand(
        Long reviewId,
        String userId,
        String role
) {
}

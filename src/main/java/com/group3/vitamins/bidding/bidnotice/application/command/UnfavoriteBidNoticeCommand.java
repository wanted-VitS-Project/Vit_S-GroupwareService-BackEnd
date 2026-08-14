package com.group3.vitamins.bidding.bidnotice.application.command;

public record UnfavoriteBidNoticeCommand(
        Long noticeId,
        String userId,
        String role
) {
}

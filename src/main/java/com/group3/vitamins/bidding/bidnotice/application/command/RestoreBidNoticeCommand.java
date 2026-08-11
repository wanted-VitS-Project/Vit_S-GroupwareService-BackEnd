package com.group3.vitamins.bidding.bidnotice.application.command;

public record RestoreBidNoticeCommand(
        Long noticeId,
        String userId,
        String role
) {
}

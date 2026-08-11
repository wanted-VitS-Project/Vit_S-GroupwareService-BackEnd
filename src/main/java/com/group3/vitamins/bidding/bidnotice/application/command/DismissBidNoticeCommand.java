package com.group3.vitamins.bidding.bidnotice.application.command;

public record DismissBidNoticeCommand(
        Long noticeId,
        String reason,
        String userId,
        String role
) {
}

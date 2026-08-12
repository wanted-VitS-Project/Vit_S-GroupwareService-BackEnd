package com.group3.vitamins.bidding.bidsummary.application.command;

public record CreateBidNoticeSummaryCommand(
        Long noticeId,
        String userId,
        String role,
        String prompt
) {
}
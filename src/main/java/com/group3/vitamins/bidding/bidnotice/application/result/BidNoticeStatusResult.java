package com.group3.vitamins.bidding.bidnotice.application.result;

import java.time.LocalDateTime;

public record BidNoticeStatusResult(
        Long noticeId,
        String noticeStatus,
        String dismissReason,
        LocalDateTime updatedAt
) {
}

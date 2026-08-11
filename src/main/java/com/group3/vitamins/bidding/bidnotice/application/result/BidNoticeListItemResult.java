package com.group3.vitamins.bidding.bidnotice.application.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidNoticeListItemResult(
        Long noticeId, String noticeName, String sourceCode, String sourceName,
        String sourceUrl, String noticeAgency, Long businessCategoryId,
        String businessCategoryName, BigDecimal baseAmount, BigDecimal estimatedAmount,
        LocalDateTime announcedAt, LocalDateTime bidDeadlineAt, Integer dDay,
        boolean isNew, String noticeStatus, Long projectId
) {
}

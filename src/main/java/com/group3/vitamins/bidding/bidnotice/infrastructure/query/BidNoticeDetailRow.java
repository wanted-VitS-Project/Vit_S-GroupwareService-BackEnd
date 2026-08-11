package com.group3.vitamins.bidding.bidnotice.infrastructure.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidNoticeDetailRow(
        Long noticeId, String externalId, String noticeOrder, String noticeName,
        String noticeType, String externalNoticeStatus, String noticeAgency,
        String demandAgency, String noticeStatus, String dismissReason, Long projectId,
        String sourceCode, String sourceName, String sourceUrl, boolean hasAttachment,
        LocalDateTime announcedAt, LocalDateTime bidStartAt,
        LocalDateTime questionDeadlineAt, LocalDateTime applicationDeadlineAt,
        LocalDateTime bidDeadlineAt, LocalDateTime openingAt, Integer dDay,
        BigDecimal baseAmount, BigDecimal estimatedAmount, String priceRangeText,
        String minimumBidRateText, String participationQualificationText,
        String regionLimitText, String businessLimitText, Boolean jointContractAllowed,
        String jointContractText, String contractMethod, String evaluationMethod
) {
}

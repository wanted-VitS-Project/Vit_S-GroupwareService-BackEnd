package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper;

import java.time.LocalDateTime;

public record CompanyBidNoticeStateRow(
        Long companyId,
        Long bidNoticeId,
        String noticeStatus,
        String dismissReason,
        boolean isFavorite,
        LocalDateTime updatedAt
) {
}

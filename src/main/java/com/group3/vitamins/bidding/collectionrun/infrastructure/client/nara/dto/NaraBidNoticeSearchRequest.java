package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto;

import java.time.LocalDateTime;

public record NaraBidNoticeSearchRequest(
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String keyword,
        String regionCode,
        String industryCode,
        Long minimumEstimatedPrice,
        Long maximumEstimatedPrice,
        Boolean excludeClosed,
        String internationalBidType,
        int pageNumber,
        int pageSize
) {
}
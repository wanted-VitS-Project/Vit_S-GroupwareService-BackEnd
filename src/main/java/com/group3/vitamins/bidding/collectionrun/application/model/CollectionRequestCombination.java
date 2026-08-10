package com.group3.vitamins.bidding.collectionrun.application.model;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;

public record CollectionRequestCombination(
        BidNoticeType noticeType,
        String keyword,
        String regionCode,
        String industryCode,
        int pageNumber
) {
}
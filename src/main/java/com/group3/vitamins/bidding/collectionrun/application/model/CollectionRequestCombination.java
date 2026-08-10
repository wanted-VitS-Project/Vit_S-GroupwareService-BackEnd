package com.group3.vitamins.bidding.collectionrun.application.model;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;

import java.util.Objects;

public record CollectionRequestCombination(
        BidNoticeType noticeType,
        String keyword,
        String regionCode,
        String industryCode,
        int pageNumber
) {
    public CollectionRequestCombination {
        Objects.requireNonNull(noticeType, "noticeType은 필수입니다.");
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber는 1 이상이어야 합니다.");
        }
    }
}

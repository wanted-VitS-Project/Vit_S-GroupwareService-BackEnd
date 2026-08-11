package com.group3.vitamins.bidding.collectionrun.application.model;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;

import java.util.List;

public record CollectedBidNoticePage(
        List<CollectedBidNoticePayload> notices,
        List<CollectionFailure> failures,
        int pageNumber,
        int reportedTotalCount,
        boolean hasNext
) {

    public CollectedBidNoticePage {
        notices = notices == null ? List.of() : List.copyOf(notices);
        failures = failures == null ? List.of() : List.copyOf(failures);
        if (pageNumber < 1) {
            throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
        }
        if (reportedTotalCount < 0) {
            throw new IllegalArgumentException("외부 수집 건수는 음수일 수 없습니다.");
        }
    }

    public record CollectionFailure(
            BidNoticeType noticeType,
            String keyword,
            String regionCode,
            String industryCode,
            int pageNumber,
            CollectionRunFailureType failureType,
            boolean retryable
    ) {
    }
}

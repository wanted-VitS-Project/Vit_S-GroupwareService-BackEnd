package com.group3.vitamins.bidding.bidsummary.domain.model;

public enum BidNoticeSummaryStatus {

    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    // 같은 사용자에게 새로운 요약 요청을 허용할지 판단합니다.
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING;
    }
}
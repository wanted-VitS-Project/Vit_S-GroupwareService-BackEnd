package com.group3.vitamins.bidding.bidreview.domain.model;

public enum BidReviewStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    ABANDONED,
    EXPIRED;

    public boolean isProcessing() {
        return this == PENDING || this == PROCESSING;
    }

    public boolean isAbandonable() {
        return this == PENDING
                || this == PROCESSING
                || this == COMPLETED
                || this == FAILED;
    }

    public boolean acceptsWorkerCallback() {
        return this == PENDING || this == PROCESSING;
    }
}
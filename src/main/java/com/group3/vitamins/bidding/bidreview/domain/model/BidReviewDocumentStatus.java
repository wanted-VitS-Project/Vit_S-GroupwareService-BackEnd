package com.group3.vitamins.bidding.bidreview.domain.model;

public enum BidReviewDocumentStatus {
    PENDING,
    DOWNLOADING,
    READY,
    FAILED,
    PROMOTED,
    DELETED;

    public boolean isTemporaryFileCleanupTarget() {
        return this == DOWNLOADING
                || this == READY
                || this == FAILED;
    }
}
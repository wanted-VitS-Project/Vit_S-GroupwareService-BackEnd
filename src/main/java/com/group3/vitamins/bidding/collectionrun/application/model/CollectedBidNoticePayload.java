package com.group3.vitamins.bidding.collectionrun.application.model;

import java.util.Objects;

public record CollectedBidNoticePayload(
        CollectedBidNotice notice,
        String rawPayload,
        String rawPayloadHash
) {

    public CollectedBidNoticePayload {
        Objects.requireNonNull(notice, "notice must not be null");

        if (rawPayload == null || rawPayload.isBlank()) {
            throw new IllegalArgumentException("rawPayload must not be blank");
        }

        if (rawPayloadHash == null
                || !rawPayloadHash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "rawPayloadHash must be a lowercase SHA-256 hash"
            );
        }
    }
}
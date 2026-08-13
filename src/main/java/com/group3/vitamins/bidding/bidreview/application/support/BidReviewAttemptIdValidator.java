package com.group3.vitamins.bidding.bidreview.application.support;

import java.util.UUID;

public final class BidReviewAttemptIdValidator {

    private BidReviewAttemptIdValidator() {
    }

    // Worker 시도 ID가 UUID 형식인지 확인합니다.
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            UUID parsed = UUID.fromString(value);
            return parsed.toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

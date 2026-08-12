package com.group3.vitamins.bidding.bidsummary.application.support;

import java.util.UUID;

public final class BidNoticeSummaryAttemptIdValidator {

    private BidNoticeSummaryAttemptIdValidator() {
    }

    // Worker 시도 ID가 UUID 형식인지 확인합니다.
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

package com.group3.vitamins.bidding.bidsummary.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BidNoticeSummaryAttemptIdValidatorTest {

    @Test
    @DisplayName("표준 UUID 문자열을 허용한다")
    void acceptsCanonicalUuid() {
        assertThat(BidNoticeSummaryAttemptIdValidator.isValid(
                "e0120882-8b6b-4cfc-9c3a-015cc3202ae6"
        )).isTrue();
    }

    @Test
    @DisplayName("축약된 비표준 UUID 문자열을 거부한다")
    void rejectsNonCanonicalUuid() {
        assertThat(BidNoticeSummaryAttemptIdValidator.isValid("1-1-1-1-1"))
                .isFalse();
    }

    @Test
    @DisplayName("null, 공백, UUID가 아닌 문자열을 거부한다")
    void rejectsInvalidValues() {
        assertThat(BidNoticeSummaryAttemptIdValidator.isValid(null)).isFalse();
        assertThat(BidNoticeSummaryAttemptIdValidator.isValid(" ")).isFalse();
        assertThat(BidNoticeSummaryAttemptIdValidator.isValid("not-a-uuid"))
                .isFalse();
    }
}

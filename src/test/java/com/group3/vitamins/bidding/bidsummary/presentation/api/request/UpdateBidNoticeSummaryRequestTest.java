package com.group3.vitamins.bidding.bidsummary.presentation.api.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateBidNoticeSummaryRequest PATCH 필드 구분")
class UpdateBidNoticeSummaryRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("생략한 필드와 전달한 문자열을 구분한다")
    void distinguishesAbsentAndPresentFields() throws Exception {
        var body = objectMapper.readTree("{\"riskSummary\":\"수정된 위험\"}");

        var command = UpdateBidNoticeSummaryRequest.toCommand(
                31L, body, "vitas-USER001", "ADMIN"
        );

        assertThat(command.overviewSummary().present()).isFalse();
        assertThat(command.riskSummary().present()).isTrue();
        assertThat(command.riskSummary().value()).isEqualTo("수정된 위험");
    }

    @Test
    @DisplayName("명시적 null과 계약에 없는 필드는 거부한다")
    void rejectsNullAndUnknownFields() throws Exception {
        assertInvalid("{\"riskSummary\":null}");
        assertInvalid("{\"unknownSummary\":\"값\"}");
    }

    private void assertInvalid(String json) throws Exception {
        assertThatThrownBy(() -> UpdateBidNoticeSummaryRequest.toCommand(
                31L, objectMapper.readTree(json), "vitas-USER001", "ADMIN"
        )).isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_INVALID_SUMMARY_UPDATE));
    }
}

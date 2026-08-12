package com.group3.vitamins.bidding.bidsummary.presentation.api.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.bidsummary.application.command.SummaryPatchField;
import com.group3.vitamins.bidding.bidsummary.application.command.UpdateBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "입찰 공고 AI 요약 부분 수정 요청")
public record UpdateBidNoticeSummaryRequest(
        @Schema(description = "공고 개요 수정값")
        String overviewSummary,
        @Schema(description = "금액 요약 수정값")
        String amountSummary,
        @Schema(description = "일정 요약 수정값")
        String scheduleSummary,
        @Schema(description = "참가 자격 요약 수정값")
        String qualificationSummary,
        @Schema(description = "주요 과업 요약 수정값")
        String taskSummary,
        @Schema(description = "위험 요소 요약 수정값")
        String riskSummary
) {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "overviewSummary", "amountSummary", "scheduleSummary",
            "qualificationSummary", "taskSummary", "riskSummary"
    );

    public static UpdateBidNoticeSummaryCommand toCommand(
            Long summaryId,
            JsonNode body,
            String userId,
            String role
    ) {
        if (body == null || !body.isObject()) {
            throw invalid();
        }
        body.fieldNames().forEachRemaining(field -> {
            if (!ALLOWED_FIELDS.contains(field)) {
                throw invalid();
            }
        });

        return new UpdateBidNoticeSummaryCommand(
                summaryId,
                patch(body, "overviewSummary"),
                patch(body, "amountSummary"),
                patch(body, "scheduleSummary"),
                patch(body, "qualificationSummary"),
                patch(body, "taskSummary"),
                patch(body, "riskSummary"),
                userId,
                role
        );
    }

    private static SummaryPatchField patch(JsonNode body, String fieldName) {
        if (!body.has(fieldName)) {
            return SummaryPatchField.absent();
        }
        JsonNode value = body.get(fieldName);
        if (value == null || !value.isTextual()) {
            throw invalid();
        }
        return SummaryPatchField.of(value.textValue());
    }

    private static ValidationException invalid() {
        return new ValidationException(
                BiddingErrorCode.BIDDING_INVALID_SUMMARY_UPDATE
        );
    }
}

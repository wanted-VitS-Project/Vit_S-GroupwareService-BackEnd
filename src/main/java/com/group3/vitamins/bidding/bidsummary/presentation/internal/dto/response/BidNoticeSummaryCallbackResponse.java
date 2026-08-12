package com.group3.vitamins.bidding.bidsummary.presentation.internal.dto.response;

import com.group3.vitamins.bidding.bidsummary.application.result
        .BidNoticeSummaryCallbackResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record BidNoticeSummaryCallbackResponse(

        @Schema(description = "callback 결과 반영 여부")
        boolean accepted,

        @Schema(description = "AI 요약 ID")
        Long summaryId,

        @Schema(description = "현재 AI 요약 상태")
        String summaryStatus,

        @Schema(description = "반영하지 않은 이유")
        String reason
) {

    public static BidNoticeSummaryCallbackResponse from(
            BidNoticeSummaryCallbackResult result
    ) {
        return new BidNoticeSummaryCallbackResponse(
                result.accepted(),
                result.summaryId(),
                result.summaryStatus(),
                result.reason()
        );
    }
}
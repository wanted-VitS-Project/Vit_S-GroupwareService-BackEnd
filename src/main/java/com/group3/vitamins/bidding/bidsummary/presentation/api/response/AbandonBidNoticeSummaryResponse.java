package com.group3.vitamins.bidding.bidsummary.presentation.api.response;

import com.group3.vitamins.bidding.bidsummary.application.result.AbandonBidNoticeSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AbandonBidNoticeSummaryResponse(

        @Schema(description = "AI 요약 ID", example = "31")
        Long summaryId,

        @Schema(description = "요약 처리 상태", example = "ABANDONED")
        String summaryStatus,

        @Schema(description = "중단 처리 시각")
        LocalDateTime abandonedAt
) {

    public static AbandonBidNoticeSummaryResponse from(AbandonBidNoticeSummaryResult result) {
        return new AbandonBidNoticeSummaryResponse(
                result.summaryId(),
                result.summaryStatus(),
                result.abandonedAt()
        );
    }
}

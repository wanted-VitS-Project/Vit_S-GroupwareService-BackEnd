package com.group3.vitamins.bidding.bidsummary.presentation.api.response;

import com.group3.vitamins.bidding.bidsummary.application.result.CreateBidNoticeSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CreateBidNoticeSummaryResponse(

        @Schema(description = "입찰 공고 AI 요약 ID", example = "31")
        Long summaryId,

        @Schema(description = "AI 요약 처리 상태", example = "PENDING")
        String summaryStatus,

        @Schema(
                description = "AI 요약 요청 접수 시각",
                example = "2026-08-11T17:30:00"
        )
        LocalDateTime requestedAt
) {

    public static CreateBidNoticeSummaryResponse from(
            CreateBidNoticeSummaryResult result
    ) {
        return new CreateBidNoticeSummaryResponse(
                result.summaryId(),
                result.summaryStatus(),
                result.requestedAt()
        );
    }
}
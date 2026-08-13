package com.group3.vitamins.bidding.bidreview.presentation.api.response;

import com.group3.vitamins.bidding.bidreview.application.result.AbandonBidReviewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AbandonBidReviewResponse(

        @Schema(description = "검토 ID", example = "71")
        Long reviewId,

        @Schema(description = "검토 상태", example = "ABANDONED")
        String reviewStatus,

        @Schema(description = "종료 처리 시각")
        LocalDateTime abandonedAt
) {

    public static AbandonBidReviewResponse from(AbandonBidReviewResult result) {
        return new AbandonBidReviewResponse(
                result.reviewId(),
                result.reviewStatus(),
                result.abandonedAt()
        );
    }
}

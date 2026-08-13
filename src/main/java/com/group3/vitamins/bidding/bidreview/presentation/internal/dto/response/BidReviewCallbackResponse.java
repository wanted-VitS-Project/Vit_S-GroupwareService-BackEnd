package com.group3.vitamins.bidding.bidreview.presentation.internal.dto.response;

import com.group3.vitamins.bidding.bidreview.application.result.BidReviewCallbackResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record BidReviewCallbackResponse(

        @Schema(description = "callback 결과 반영 여부")
        boolean accepted,

        @Schema(description = "검토 ID")
        Long reviewId,

        @Schema(description = "현재 검토 상태")
        String reviewStatus,

        @Schema(description = "반영하지 않은 이유")
        String reason
) {

    public static BidReviewCallbackResponse from(BidReviewCallbackResult result) {
        return new BidReviewCallbackResponse(
                result.accepted(),
                result.reviewId(),
                result.reviewStatus(),
                result.reason()
        );
    }
}

package com.group3.vitamins.bidding.bidreview.presentation.api.response;

import com.group3.vitamins.bidding.bidreview.application.result.CreateBidReviewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CreateBidReviewResponse(

        @Schema(description = "입찰 문서 검토 ID", example = "71")
        Long reviewId,

        @Schema(description = "검토 처리 상태", example = "PENDING")
        String reviewStatus,

        @Schema(description = "검토 요청 접수 시각", example = "2026-08-12T14:00:00")
        LocalDateTime requestedAt
) {

    public static CreateBidReviewResponse from(CreateBidReviewResult result) {
        return new CreateBidReviewResponse(
                result.reviewId(),
                result.reviewStatus(),
                result.requestedAt()
        );
    }
}
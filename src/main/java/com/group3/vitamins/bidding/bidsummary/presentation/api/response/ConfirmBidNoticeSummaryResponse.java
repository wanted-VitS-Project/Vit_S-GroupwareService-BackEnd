package com.group3.vitamins.bidding.bidsummary.presentation.api.response;

import com.group3.vitamins.bidding.bidsummary.application.result.ConfirmBidNoticeSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ConfirmBidNoticeSummaryResponse(
        @Schema(description = "AI 요약 ID", example = "31") Long summaryId,
        @Schema(description = "최종 확정 여부", example = "true") boolean confirmed,
        @Schema(description = "확정자 ID", example = "vitas-USER001") String confirmedBy,
        @Schema(description = "확정 시각") LocalDateTime confirmedAt,
        @Schema(description = "프로젝트 생성 가능 여부", example = "true")
        boolean projectCreationAllowed
) {
    public static ConfirmBidNoticeSummaryResponse from(
            ConfirmBidNoticeSummaryResult result
    ) {
        return new ConfirmBidNoticeSummaryResponse(
                result.summaryId(), result.confirmed(), result.confirmedBy(),
                result.confirmedAt(), result.projectCreationAllowed()
        );
    }
}

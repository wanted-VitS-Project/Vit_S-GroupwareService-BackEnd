package com.group3.vitamins.bidding.bidsummary.presentation.internal.dto.request;

import com.group3.vitamins.bidding.bidsummary.application.command
        .HandleBidNoticeSummaryCallbackCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record BidNoticeSummaryCallbackRequest(

        @NotBlank
        @Schema(description = "현재 작업 시도 ID")
        String attemptId,

        @NotBlank
        @Schema(description = "처리 결과 상태", allowableValues = {"COMPLETED", "FAILED"})
        String summaryStatus,

        @Schema(description = "공고 전체 개요")
        String overviewSummary,

        @Schema(description = "금액 요약")
        String amountSummary,

        @Schema(description = "일정 요약")
        String scheduleSummary,

        @Schema(description = "참가 자격 요약")
        String qualificationSummary,

        @Schema(description = "주요 과업 요약")
        String taskSummary,

        @Schema(description = "위험 요소 요약")
        String riskSummary,

        @Schema(description = "실패 메시지")
        String errorMessage,

        @Schema(
                description = "일시 장애로 재시도할 수 있는지 여부",
                example = "false",
                defaultValue = "false"
        )
        Boolean retryable
) {

    // HTTP 요청을 callback 처리 Command로 변환합니다.
    public HandleBidNoticeSummaryCallbackCommand toCommand(Long summaryId) {
        return new HandleBidNoticeSummaryCallbackCommand(
                summaryId,
                attemptId,
                summaryStatus,
                overviewSummary,
                amountSummary,
                scheduleSummary,
                qualificationSummary,
                taskSummary,
                riskSummary,
                errorMessage,
                Boolean.TRUE.equals(retryable)
        );
    }
}

package com.group3.vitamins.bidding.bidsummary.presentation.internal.dto.response;

import com.group3.vitamins.bidding.bidsummary.application.port
        .BidNoticeSummaryNoticePort.BidNoticeSnapshot;
import com.group3.vitamins.bidding.bidsummary.application.result
        .BidNoticeSummaryJobResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record BidNoticeSummaryJobResponse(

        @Schema(description = "AI 요약 ID")
        Long summaryId,

        @Schema(description = "회사 ID")
        Long companyId,

        @Schema(description = "현재 작업 시도 ID")
        String attemptId,

        @Schema(description = "사용자가 입력한 프롬프트")
        String prompt,

        @Schema(description = "요약 요청 당시 입찰 공고 스냅샷")
        BidNoticeSnapshot notice
) {

    public static BidNoticeSummaryJobResponse from(
            BidNoticeSummaryJobResult result
    ) {
        return new BidNoticeSummaryJobResponse(
                result.summaryId(),
                result.companyId(),
                result.attemptId(),
                result.prompt(),
                result.notice()
        );
    }
}
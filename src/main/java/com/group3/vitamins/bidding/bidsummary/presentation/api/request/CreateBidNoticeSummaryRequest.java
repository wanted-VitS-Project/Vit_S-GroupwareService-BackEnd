package com.group3.vitamins.bidding.bidsummary.presentation.api.request;

import com.group3.vitamins.bidding.bidsummary.application.command.CreateBidNoticeSummaryCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateBidNoticeSummaryRequest(

        @Schema(
                description = "사용자가 직접 입력하는 입찰 공고 분석 요청",
                example = "공고의 금액, 일정, 참가 자격과 수행 위험을 실무 검토용으로 정리해주세요."
        )
        @NotBlank
        @Size(max = 3000)
        String prompt,

        @Schema(
                description = "개선 기준이 되는 본인의 같은 공고 미확정 완료 요약 ID",
                example = "31",
                nullable = true
        )
        @Positive
        Long baseSummaryId
) {

    public CreateBidNoticeSummaryCommand toCommand(
            Long noticeId,
            String userId,
            String role
    ) {
        return new CreateBidNoticeSummaryCommand(
                noticeId,
                userId,
                role,
                prompt,
                baseSummaryId
        );
    }
}

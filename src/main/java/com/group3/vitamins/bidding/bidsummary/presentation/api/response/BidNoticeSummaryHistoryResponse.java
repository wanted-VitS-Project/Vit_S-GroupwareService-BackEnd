package com.group3.vitamins.bidding.bidsummary.presentation.api.response;

import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryItemResult;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record BidNoticeSummaryHistoryResponse(
        @Schema(description = "현재 사용자가 요청한 최신 AI 요약 ID", nullable = true)
        Long latestMySummaryId,
        @Schema(description = "조회 가능한 AI 요약 이력")
        List<Item> content,
        @Schema(description = "전체 이력 수") long totalElements,
        @Schema(description = "전체 페이지 수") int totalPages,
        @Schema(description = "현재 페이지") int page,
        @Schema(description = "페이지 크기") int size
) {
    public static BidNoticeSummaryHistoryResponse from(
            BidNoticeSummaryHistoryResult result
    ) {
        return new BidNoticeSummaryHistoryResponse(
                result.latestMySummaryId(),
                result.content().stream().map(Item::from).toList(),
                result.totalElements(), result.totalPages(),
                result.page(), result.size()
        );
    }

    public record Item(
            @Schema(description = "AI 요약 ID") Long summaryId,
            @Schema(description = "개선 기준 AI 요약 ID", nullable = true)
            Long parentSummaryId,
            @Schema(description = "요약 개정 번호") int revisionNo,
            @Schema(description = "요약 처리 상태") String summaryStatus,
            @Schema(description = "요청 당시 프롬프트") String prompt,
            @Schema(description = "확정 여부") boolean confirmed,
            @Schema(description = "현재 사용자가 요청한 요약인지 여부") boolean isMine,
            @Schema(description = "전환된 프로젝트 ID", nullable = true) Long projectId,
            @Schema(description = "요청 시각") LocalDateTime requestedAt,
            @Schema(description = "확정 시각", nullable = true) LocalDateTime confirmedAt
    ) {
        private static Item from(BidNoticeSummaryHistoryItemResult result) {
            return new Item(
                    result.summaryId(), result.parentSummaryId(), result.revisionNo(),
                    result.summaryStatus(), result.prompt(), result.confirmed(),
                    result.mine(), result.projectId(), result.requestedAt(),
                    result.confirmedAt()
            );
        }
    }
}

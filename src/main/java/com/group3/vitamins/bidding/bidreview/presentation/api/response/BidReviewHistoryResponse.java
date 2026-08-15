package com.group3.vitamins.bidding.bidreview.presentation.api.response;

import com.group3.vitamins.bidding.bidreview.application.result.BidReviewHistoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record BidReviewHistoryResponse(

        @Schema(description = "본인이 요청한 검토 이력 최신순")
        List<HistoryItemResponse> content,

        @Schema(description = "전체 이력 수") long totalElements,

        @Schema(description = "전체 페이지 수") int totalPages,

        @Schema(description = "현재 페이지") int page,

        @Schema(description = "페이지 크기") int size
) {

    public static BidReviewHistoryResponse from(BidReviewHistoryResult result) {
        return new BidReviewHistoryResponse(
                result.content().stream()
                        .map(HistoryItemResponse::from)
                        .toList(),
                result.totalElements(),
                result.totalPages(),
                result.page(),
                result.size()
        );
    }

    public record HistoryItemResponse(

            @Schema(description = "검토 ID", example = "71")
            Long reviewId,

            @Schema(description = "검토 상태", example = "COMPLETED")
            String reviewStatus,

            @Schema(description = "요청 당시 사용자 프롬프트")
            String prompt,

            @Schema(description = "요청 시각")
            LocalDateTime requestedAt,

            @Schema(description = "완료 또는 실패 시각", nullable = true)
            LocalDateTime completedAt,

            @Schema(description = "임시파일 정리 예정 시각", nullable = true)
            LocalDateTime expiresAt,

            @Schema(description = "정식 프로젝트로 귀속됐으면 프로젝트 ID", nullable = true)
            Long projectId
    ) {

        public static HistoryItemResponse from(
                BidReviewHistoryResult.HistoryItemResult result
        ) {
            return new HistoryItemResponse(
                    result.reviewId(),
                    result.reviewStatus(),
                    result.prompt(),
                    result.requestedAt(),
                    result.completedAt(),
                    result.expiresAt(),
                    result.projectId()
            );
        }
    }
}
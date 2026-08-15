package com.group3.vitamins.bidding.bidsummary.presentation.api.response;

import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record BidNoticeSummaryResponse(
        @Schema(description = "AI 요약 ID", example = "31") Long summaryId,
        @Schema(description = "입찰 공고 ID", example = "317") Long noticeId,
        @Schema(description = "개선 기준 AI 요약 ID", nullable = true) Long parentSummaryId,
        @Schema(description = "요약 개정 번호", example = "1") int revisionNo,
        @Schema(description = "사용자 입력 프롬프트") String prompt,
        @Schema(description = "요약 처리 상태", example = "COMPLETED") String summaryStatus,
        @Schema(description = "공고 개요", nullable = true) String overviewSummary,
        @Schema(description = "금액 요약", nullable = true) String amountSummary,
        @Schema(description = "일정 요약", nullable = true) String scheduleSummary,
        @Schema(description = "참가 자격 요약", nullable = true) String qualificationSummary,
        @Schema(description = "주요 과업 요약", nullable = true) String taskSummary,
        @Schema(description = "위험 요소 요약", nullable = true) String riskSummary,
        @Schema(description = "최종 확정 여부", example = "false") boolean confirmed,
        @Schema(description = "확정자 ID", nullable = true) String confirmedBy,
        @Schema(description = "확정 시각", nullable = true) LocalDateTime confirmedAt,
        @Schema(description = "전환된 프로젝트 ID", nullable = true) Long projectId,
        @Schema(description = "실패 메시지", nullable = true) String errorMessage,
        @Schema(description = "일시 장애로 재시도한 횟수. 화면에서 \"재시도 중 (n/2)\"로 안내할 때 사용", example = "0")
        int retryCount,
        @Schema(description = "요약 요청 시각") LocalDateTime requestedAt,
        @Schema(description = "요약 완료 시각", nullable = true) LocalDateTime completedAt,
        @Schema(description = "마지막 수정 시각", nullable = true) LocalDateTime updatedAt
) {
    public static BidNoticeSummaryResponse from(BidNoticeSummaryResult result) {
        return new BidNoticeSummaryResponse(
                result.summaryId(), result.noticeId(), result.parentSummaryId(),
                result.revisionNo(), result.prompt(),
                result.summaryStatus(), result.overviewSummary(),
                result.amountSummary(), result.scheduleSummary(),
                result.qualificationSummary(), result.taskSummary(),
                result.riskSummary(), result.confirmed(),
                result.confirmedBy(), result.confirmedAt(), result.projectId(),
                result.errorMessage(), result.retryCount(), result.requestedAt(),
                result.completedAt(), result.updatedAt()
        );
    }
}

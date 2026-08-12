package com.group3.vitamins.bidding.bidsummary.application.port;

import com.group3.vitamins.bidding.bidsummary.application.port
        .BidNoticeSummaryNoticePort.BidNoticeSnapshot;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BidNoticeSummaryWorkerPort {

    // 현재 attemptId가 유효하면 작업을 점유하고 PROCESSING 상태로 전환합니다.
    Optional<JobData> claimJob(
            Long summaryId,
            String attemptId,
            LocalDateTime now
    );

    // 현재 attemptId와 일치하는 요약을 완료 처리합니다.
    CallbackUpdate complete(
            Long summaryId,
            String attemptId,
            CompletedSummary summary,
            LocalDateTime now
    );

    // 현재 attemptId와 일치하는 요약을 실패 처리합니다.
    CallbackUpdate fail(
            Long summaryId,
            String attemptId,
            String errorMessage,
            boolean retryable,
            LocalDateTime now
    );

    record JobData(
            Long summaryId,
            Long companyId,
            String attemptId,
            String prompt,
            BidNoticeSnapshot notice
    ) {
    }

    record CompletedSummary(
            String overviewSummary,
            String amountSummary,
            String scheduleSummary,
            String qualificationSummary,
            String taskSummary,
            String riskSummary
    ) {
    }

    record CallbackUpdate(
            boolean exists,
            boolean accepted,
            BidNoticeSummaryStatus currentStatus
    ) {
    }
}

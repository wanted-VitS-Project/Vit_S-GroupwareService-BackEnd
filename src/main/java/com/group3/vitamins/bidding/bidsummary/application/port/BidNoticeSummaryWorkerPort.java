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

    // 살아있는(PENDING) outbox가 하나도 없이 오래 멈춘 요약을 찾아 재큐잉하거나, 재시도를 소진했으면
    // 실패로 종료합니다. 정상 흐름에서는 발생하지 않아야 하는 상태를 방어적으로 복구하는 안전망입니다.
    // 반환값은 실제로 손댄 건수입니다.
    int recoverOrphaned(LocalDateTime staleBefore, int batchLimit, LocalDateTime now);

    record JobData(
            Long summaryId,
            Long companyId,
            String attemptId,
            String prompt,
            PreviousSummary previousSummary,
            BidNoticeSnapshot notice
    ) {
    }

    record PreviousSummary(
            Long summaryId,
            int revisionNo,
            String overviewSummary,
            String amountSummary,
            String scheduleSummary,
            String qualificationSummary,
            String taskSummary,
            String riskSummary
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

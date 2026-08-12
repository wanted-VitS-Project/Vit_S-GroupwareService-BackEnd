package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryWorkerPort;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryJobQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryJobResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.GetBidNoticeSummaryJobUseCase;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.group3.vitamins.bidding.bidsummary.application.support.BidNoticeSummaryAttemptIdValidator.isValid;

@Service
@RequiredArgsConstructor
public class BidNoticeSummaryJobQueryService
        implements GetBidNoticeSummaryJobUseCase {

    private final BidNoticeSummaryWorkerPort workerPort;
    private final Clock clock;

    @Override
    @Transactional
    public BidNoticeSummaryJobResult handle(GetBidNoticeSummaryJobQuery query) {
        validate(query);

        BidNoticeSummaryWorkerPort.JobData job = workerPort.claimJob(
                query.summaryId(),
                query.attemptId(),
                LocalDateTime.now(clock)
        ).orElseThrow(() -> new NotFoundException(
                BiddingErrorCode.BIDDING_SUMMARY_JOB_NOT_FOUND
        ));

        return new BidNoticeSummaryJobResult(
                job.summaryId(),
                job.companyId(),
                job.attemptId(),
                job.prompt(),
                job.previousSummary(),
                job.notice()
        );
    }

    private void validate(GetBidNoticeSummaryJobQuery query) {
        if (query == null
                || query.summaryId() == null
                || query.summaryId() <= 0
                || !isValid(query.attemptId())) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_INVALID_SUMMARY_JOB_REQUEST
            );
        }
    }

}

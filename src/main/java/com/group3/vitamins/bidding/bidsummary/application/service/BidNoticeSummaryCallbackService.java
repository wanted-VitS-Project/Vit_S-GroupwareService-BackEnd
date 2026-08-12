package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.command.HandleBidNoticeSummaryCallbackCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryWorkerPort;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryCallbackResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.HandleBidNoticeSummaryCallbackUseCase;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
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
public class BidNoticeSummaryCallbackService
        implements HandleBidNoticeSummaryCallbackUseCase {

    private static final String IGNORED_REASON =
            "attempt_mismatch_or_already_finished";

    private final BidNoticeSummaryWorkerPort workerPort;
    private final Clock clock;

    @Override
    @Transactional
    public BidNoticeSummaryCallbackResult handle(
            HandleBidNoticeSummaryCallbackCommand command
    ) {
        validate(command);

        BidNoticeSummaryWorkerPort.CallbackUpdate update =
                BidNoticeSummaryStatus.COMPLETED.name().equals(command.summaryStatus())
                        ? complete(command)
                        : fail(command);

        if (!update.exists()) {
            throw new NotFoundException(
                    BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND
            );
        }

        return new BidNoticeSummaryCallbackResult(
                update.accepted(),
                command.summaryId(),
                update.currentStatus().name(),
                update.accepted() ? null : IGNORED_REASON
        );
    }

    private BidNoticeSummaryWorkerPort.CallbackUpdate complete(
            HandleBidNoticeSummaryCallbackCommand command
    ) {
        return workerPort.complete(
                command.summaryId(),
                command.attemptId(),
                new BidNoticeSummaryWorkerPort.CompletedSummary(
                        command.overviewSummary(),
                        command.amountSummary(),
                        command.scheduleSummary(),
                        command.qualificationSummary(),
                        command.taskSummary(),
                        command.riskSummary()
                ),
                LocalDateTime.now(clock)
        );
    }

    private BidNoticeSummaryWorkerPort.CallbackUpdate fail(
            HandleBidNoticeSummaryCallbackCommand command
    ) {
        return workerPort.fail(
                command.summaryId(),
                command.attemptId(),
                command.errorMessage(),
                command.retryable(),
                LocalDateTime.now(clock)
        );
    }

    private void validate(HandleBidNoticeSummaryCallbackCommand command) {
        if (command == null
                || command.summaryId() == null
                || command.summaryId() <= 0
                || !isValid(command.attemptId())) {
            invalid();
        }

        if (BidNoticeSummaryStatus.COMPLETED.name().equals(command.summaryStatus())) {
            validateCompleted(command);
            return;
        }

        if (BidNoticeSummaryStatus.FAILED.name().equals(command.summaryStatus())) {
            validateFailed(command);
            return;
        }

        invalid();
    }

    private void validateCompleted(HandleBidNoticeSummaryCallbackCommand command) {
        if (isBlank(command.overviewSummary())
                || command.errorMessage() != null
                || command.retryable()) {
            invalid();
        }
    }

    private void validateFailed(HandleBidNoticeSummaryCallbackCommand command) {
        if (isBlank(command.errorMessage())
                || command.overviewSummary() != null
                || command.amountSummary() != null
                || command.scheduleSummary() != null
                || command.qualificationSummary() != null
                || command.taskSummary() != null
                || command.riskSummary() != null) {
            invalid();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void invalid() {
        throw new ValidationException(
                BiddingErrorCode.BIDDING_INVALID_SUMMARY_CALLBACK
        );
    }
}

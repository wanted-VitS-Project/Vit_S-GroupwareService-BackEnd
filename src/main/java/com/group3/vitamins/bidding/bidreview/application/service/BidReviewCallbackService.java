package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.command.HandleBidReviewCallbackCommand;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewWorkerPort;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewCallbackResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.HandleBidReviewCallbackUseCase;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static com.group3.vitamins.bidding.bidreview.application.support.BidReviewAttemptIdValidator.isValid;

@Service
@RequiredArgsConstructor
public class BidReviewCallbackService implements HandleBidReviewCallbackUseCase {

    private static final String PROCESSING = "PROCESSING";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";
    private static final String DEFAULT_IGNORED_REASON =
            "attempt_mismatch_or_already_finished";

    private final BidReviewWorkerPort workerPort;
    private final Clock clock;

    @Override
    @Transactional
    public BidReviewCallbackResult handle(HandleBidReviewCallbackCommand command) {
        validate(command);

        List<BidReviewWorkerPort.DocumentOutcome> documents =
                toDocumentOutcomes(command.documents());
        LocalDateTime now = LocalDateTime.now(clock);

        BidReviewWorkerPort.CallbackUpdate update;
        if (PROCESSING.equals(command.reviewStatus())) {
            update = workerPort.reportProgress(
                    command.reviewId(),
                    command.attemptId(),
                    documents,
                    now
            );
        } else if (COMPLETED.equals(command.reviewStatus())) {
            update = workerPort.complete(
                    command.reviewId(),
                    command.attemptId(),
                    command.result(),
                    documents,
                    toCitationInputs(command.citations()),
                    now
            );
        } else {
            // validate()가 PROCESSING/COMPLETED/FAILED 셋만 통과시키므로 여기 도달하면 FAILED다.
            update = workerPort.fail(
                    command.reviewId(),
                    command.attemptId(),
                    command.errorCode(),
                    command.errorMessage(),
                    command.retryable(),
                    documents,
                    now
            );
        }

        if (!update.exists()) {
            throw new NotFoundException(
                    BidReviewErrorCode.BIDDING_REVIEW_NOT_FOUND
            );
        }

        return new BidReviewCallbackResult(
                update.accepted(),
                command.reviewId(),
                update.currentStatus(),
                update.accepted()
                        ? null
                        : (update.reason() != null ? update.reason() : DEFAULT_IGNORED_REASON)
        );
    }

    private List<BidReviewWorkerPort.DocumentOutcome> toDocumentOutcomes(
            List<HandleBidReviewCallbackCommand.DocumentOutcomeInput> documents
    ) {
        if (documents == null) {
            return List.of();
        }

        return documents.stream()
                .map(document -> new BidReviewWorkerPort.DocumentOutcome(
                        document.bidAttachmentId(),
                        document.processingStatus(),
                        document.temporaryStorageKey(),
                        document.fileSize(),
                        document.mimeType()
                ))
                .toList();
    }

    private List<BidReviewWorkerPort.CitationInput> toCitationInputs(
            List<HandleBidReviewCallbackCommand.CitationInputCommand> citations
    ) {
        if (citations == null) {
            return List.of();
        }

        return citations.stream()
                .map(citation -> new BidReviewWorkerPort.CitationInput(
                        citation.rankOrder(),
                        citation.documentRole(),
                        citation.bidAttachmentId(),
                        citation.referenceFileId(),
                        citation.fileName(),
                        citation.pageNumber(),
                        citation.sheetName(),
                        citation.excerpt()
                ))
                .toList();
    }

    private void validate(HandleBidReviewCallbackCommand command) {
        if (command == null
                || command.reviewId() == null
                || command.reviewId() <= 0
                || !isValid(command.attemptId())) {
            invalid();
        }

        if (PROCESSING.equals(command.reviewStatus())) {
            validateProcessing(command);
            return;
        }

        if (COMPLETED.equals(command.reviewStatus())) {
            validateCompleted(command);
            return;
        }

        if (FAILED.equals(command.reviewStatus())) {
            validateFailed(command);
            return;
        }

        invalid();
    }

    private void validateProcessing(HandleBidReviewCallbackCommand command) {
        if (command.result() != null
                || command.errorCode() != null
                || command.errorMessage() != null
                || command.retryable()
                || hasCitations(command)) {
            invalid();
        }
    }

    private void validateCompleted(HandleBidReviewCallbackCommand command) {
        if (isBlank(command.result())
                || command.errorCode() != null
                || command.errorMessage() != null
                || command.retryable()) {
            invalid();
        }
    }

    private void validateFailed(HandleBidReviewCallbackCommand command) {
        if (isBlank(command.errorMessage())
                || command.errorMessage().length() > 500
                || command.result() != null
                || hasCitations(command)) {
            invalid();
        }
    }

    private boolean hasCitations(HandleBidReviewCallbackCommand command) {
        return command.citations() != null && !command.citations().isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void invalid() {
        throw new ValidationException(
                BidReviewErrorCode.BIDDING_INVALID_REVIEW_CALLBACK
        );
    }
}

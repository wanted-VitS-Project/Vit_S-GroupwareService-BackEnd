package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.command.ConfirmBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.command.SummaryPatchField;
import com.group3.vitamins.bidding.bidsummary.application.command.UpdateBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryManagementPort;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.result.ConfirmBidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.ConfirmBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.UpdateBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Service
@Transactional
@RequiredArgsConstructor
public class BidNoticeSummaryReviewService
        implements UpdateBidNoticeSummaryUseCase, ConfirmBidNoticeSummaryUseCase {

    private final BidNoticeSummaryManagementPort managementPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    @Override
    public BidNoticeSummaryResult update(UpdateBidNoticeSummaryCommand command) {
        validateUpdate(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        BidNoticeSummaryDetails current = findOwned(
                currentCompanyIdProvider.currentCompanyId(),
                command.summaryId(),
                command.userId()
        );
        if (current.summaryStatus() != BidNoticeSummaryStatus.COMPLETED
                || current.confirmed()) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_SUMMARY_NOT_EDITABLE
            );
        }

        var values = new BidNoticeSummaryManagementPort.SummaryValues(
                command.overviewSummary().resolve(current.overviewSummary()),
                command.amountSummary().resolve(current.amountSummary()),
                command.scheduleSummary().resolve(current.scheduleSummary()),
                command.qualificationSummary().resolve(current.qualificationSummary()),
                command.taskSummary().resolve(current.taskSummary()),
                command.riskSummary().resolve(current.riskSummary())
        );
        return BidNoticeSummaryResult.from(managementPort.updateSummaries(
                command.summaryId(), values, LocalDateTime.now(clock)
        ));
    }

    @Override
    public ConfirmBidNoticeSummaryResult confirm(
            ConfirmBidNoticeSummaryCommand command
    ) {
        validateConfirm(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        BidNoticeSummaryDetails current = findOwned(
                currentCompanyIdProvider.currentCompanyId(),
                command.summaryId(),
                command.userId()
        );
        if (current.confirmed()) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_SUMMARY_ALREADY_CONFIRMED
            );
        }
        if (current.summaryStatus() != BidNoticeSummaryStatus.COMPLETED) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_SUMMARY_NOT_COMPLETED
            );
        }

        BidNoticeSummaryDetails confirmed = managementPort.confirm(
                command.summaryId(), command.userId(), LocalDateTime.now(clock)
        );
        return new ConfirmBidNoticeSummaryResult(
                confirmed.summaryId(), confirmed.confirmed(),
                confirmed.confirmedBy(), confirmed.confirmedAt(), true
        );
    }

    private BidNoticeSummaryDetails findOwned(
            Long companyId, Long summaryId, String userId
    ) {
        return managementPort.findOwnedForUpdate(companyId, summaryId, userId)
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND
                ));
    }

    private void validateUpdate(UpdateBidNoticeSummaryCommand command) {
        if (command == null || command.summaryId() == null || command.summaryId() <= 0
                || command.userId() == null || command.userId().isBlank()
                || !command.hasChanges()
                || Stream.of(
                        command.overviewSummary(), command.amountSummary(),
                        command.scheduleSummary(), command.qualificationSummary(),
                        command.taskSummary(), command.riskSummary()
                ).anyMatch(this::invalidPatchValue)) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_INVALID_SUMMARY_UPDATE
            );
        }
    }

    private boolean invalidPatchValue(SummaryPatchField field) {
        return field.present()
                && (field.value() == null || field.value().isBlank());
    }

    private void validateConfirm(ConfirmBidNoticeSummaryCommand command) {
        if (command == null || command.summaryId() == null || command.summaryId() <= 0
                || command.userId() == null || command.userId().isBlank()) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_INVALID_SUMMARY_REQUEST
            );
        }
    }
}

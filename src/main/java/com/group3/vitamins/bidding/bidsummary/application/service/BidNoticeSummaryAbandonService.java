package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.command.AbandonBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryManagementPort;
import com.group3.vitamins.bidding.bidsummary.application.result.AbandonBidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.AbandonBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;
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

// 검토(bidreview)의 abandon과 동일한 형태 - 사용자가 진행 중인 AI 요약을 스스로 끝낼 수 있게 한다.
@Service
@Transactional
@RequiredArgsConstructor
public class BidNoticeSummaryAbandonService implements AbandonBidNoticeSummaryUseCase {

    private final BidNoticeSummaryManagementPort managementPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    @Override
    public AbandonBidNoticeSummaryResult abandon(AbandonBidNoticeSummaryCommand command) {
        validate(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        BidNoticeSummaryDetails current = managementPort
                .findOwnedForUpdate(
                        currentCompanyIdProvider.currentCompanyId(),
                        command.summaryId(),
                        command.userId()
                )
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND
                ));

        try {
            BidNoticeSummaryDetails abandoned = managementPort.abandon(
                    current.summaryId(), LocalDateTime.now(clock)
            );
            return AbandonBidNoticeSummaryResult.from(abandoned);
        } catch (IllegalStateException exception) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_SUMMARY_NOT_ABANDONABLE
            );
        }
    }

    private void validate(AbandonBidNoticeSummaryCommand command) {
        if (command == null || command.summaryId() == null || command.summaryId() <= 0
                || command.userId() == null || command.userId().isBlank()) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_INVALID_SUMMARY_REQUEST
            );
        }
    }
}

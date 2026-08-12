package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryManagementPort;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.GetBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BidNoticeSummaryQueryService implements GetBidNoticeSummaryUseCase {

    private final BidNoticeSummaryManagementPort managementPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public BidNoticeSummaryResult get(GetBidNoticeSummaryQuery query) {
        validate(query);
        biddingAccessPolicy.assertAccess(query.userId(), query.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        return managementPort.findAccessible(companyId, query.summaryId(), query.userId())
                .map(BidNoticeSummaryResult::from)
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND
                ));
    }

    private void validate(GetBidNoticeSummaryQuery query) {
        if (query == null || query.summaryId() == null || query.summaryId() <= 0
                || query.userId() == null || query.userId().isBlank()) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_INVALID_SUMMARY_REQUEST
            );
        }
    }
}

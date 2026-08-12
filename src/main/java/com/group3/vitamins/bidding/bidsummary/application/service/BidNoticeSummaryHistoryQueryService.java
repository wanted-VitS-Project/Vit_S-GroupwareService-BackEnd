package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryHistoryQueryPort;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryHistoryQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.GetBidNoticeSummaryHistoryUseCase;
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
public class BidNoticeSummaryHistoryQueryService
        implements GetBidNoticeSummaryHistoryUseCase {

    private static final int MAX_PAGE_SIZE = 50;

    private final BidNoticeSummaryHistoryQueryPort historyQueryPort;
    private final BidNoticeSummaryNoticePort noticePort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public BidNoticeSummaryHistoryResult get(GetBidNoticeSummaryHistoryQuery query) {
        validate(query);
        biddingAccessPolicy.assertAccess(query.userId(), query.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        noticePort.findAccessibleNotice(companyId, query.noticeId())
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                ));

        int offset = query.page() * query.size();
        var content = historyQueryPort.findHistory(
                companyId, query.noticeId(), query.userId(), offset, query.size()
        );
        long totalElements = historyQueryPort.countHistory(
                companyId, query.noticeId(), query.userId()
        );
        int totalPages = (int) Math.ceil((double) totalElements / query.size());

        return new BidNoticeSummaryHistoryResult(
                historyQueryPort.findLatestMineSummaryId(
                        companyId, query.noticeId(), query.userId()
                ),
                content,
                totalElements,
                totalPages,
                query.page(),
                query.size()
        );
    }

    private void validate(GetBidNoticeSummaryHistoryQuery query) {
        if (query == null || query.noticeId() == null || query.noticeId() <= 0
                || query.page() < 0 || query.size() <= 0
                || query.size() > MAX_PAGE_SIZE
                || query.page() > Integer.MAX_VALUE / query.size()
                || query.userId() == null || query.userId().isBlank()) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_INVALID_SUMMARY_REQUEST
            );
        }
    }
}

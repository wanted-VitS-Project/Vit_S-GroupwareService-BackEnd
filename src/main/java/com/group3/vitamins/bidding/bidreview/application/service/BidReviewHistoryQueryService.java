package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewHistoryQueryPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewHistoryQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewHistoryResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewHistoryResult.HistoryItemResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewHistoryUseCase;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BidReviewHistoryQueryService implements GetBidReviewHistoryUseCase {

    private final BidReviewNoticeDocumentPort noticeDocumentPort;
    private final BidReviewHistoryQueryPort historyQueryPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public BidReviewHistoryResult get(GetBidReviewHistoryQuery query) {
        biddingAccessPolicy.assertAccess(query.userId(), query.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        noticeDocumentPort.findAccessibleNotice(companyId, query.noticeId())
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                ));

        List<HistoryItemResult> content = historyQueryPort
                .findHistory(companyId, query.noticeId(), query.userId())
                .stream()
                .map(row -> new HistoryItemResult(
                        row.reviewId(),
                        row.reviewStatus(),
                        row.prompt(),
                        row.requestedAt(),
                        row.completedAt(),
                        row.expiresAt(),
                        row.projectId()
                ))
                .toList();

        return new BidReviewHistoryResult(content);
    }
}
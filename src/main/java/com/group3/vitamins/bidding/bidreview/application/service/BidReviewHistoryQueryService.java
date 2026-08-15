package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewHistoryQueryPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewHistoryQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewHistoryResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewHistoryResult.HistoryItemResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewHistoryUseCase;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BidReviewHistoryQueryService implements GetBidReviewHistoryUseCase {

    private static final int MAX_PAGE_SIZE = 50;

    private final BidReviewNoticeDocumentPort noticeDocumentPort;
    private final BidReviewHistoryQueryPort historyQueryPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public BidReviewHistoryResult get(GetBidReviewHistoryQuery query) {
        validate(query);
        biddingAccessPolicy.assertAccess(query.userId(), query.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        noticeDocumentPort.findAccessibleNotice(companyId, query.noticeId())
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                ));

        long totalElements = historyQueryPort.countHistory(companyId, query.noticeId(), query.userId());
        int totalPages = calculateTotalPages(totalElements, query.size());

        if (query.page() >= totalPages) {
            return new BidReviewHistoryResult(List.of(), totalElements, totalPages, query.page(), query.size());
        }

        int offset = Math.multiplyExact(query.page(), query.size());
        List<HistoryItemResult> content = historyQueryPort
                .findHistory(companyId, query.noticeId(), query.userId(), offset, query.size())
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

        return new BidReviewHistoryResult(content, totalElements, totalPages, query.page(), query.size());
    }

    private int calculateTotalPages(long totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }

        long pages = ((totalElements - 1) / size) + 1;
        return pages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pages;
    }

    private void validate(GetBidReviewHistoryQuery query) {
        if (query.noticeId() == null || query.noticeId() <= 0
                || query.page() < 0 || query.size() <= 0
                || query.size() > MAX_PAGE_SIZE
                || query.userId() == null || query.userId().isBlank()) {
            throw new ValidationException(BidReviewErrorCode.BIDDING_INVALID_REVIEW_REQUEST);
        }
    }
}
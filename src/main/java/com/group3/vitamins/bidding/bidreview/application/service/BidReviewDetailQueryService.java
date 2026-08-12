package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewDetailQueryPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewDetailQueryPort.ReviewRow;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewDetailQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewDetailResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewDetailResult.CitationResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewDetailResult.DocumentResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewDetailUseCase;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BidReviewDetailQueryService implements GetBidReviewDetailUseCase {

    private final BidReviewDetailQueryPort detailQueryPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public BidReviewDetailResult get(GetBidReviewDetailQuery query) {
        biddingAccessPolicy.assertAccess(query.userId(), query.role());

        ReviewRow review = detailQueryPort.findReview(query.reviewId())
                .orElseThrow(() -> new NotFoundException(
                        BidReviewErrorCode.BIDDING_REVIEW_NOT_FOUND
                ));

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        if (!review.companyId().equals(companyId)
                || !review.requestedBy().equals(query.userId())) {
            throw new ForbiddenException(
                    BidReviewErrorCode.BIDDING_REVIEW_ACCESS_DENIED
            );
        }

        List<DocumentResult> documents = detailQueryPort
                .findDocuments(query.reviewId())
                .stream()
                .map(row -> new DocumentResult(
                        row.documentRole(),
                        row.bidAttachmentId(),
                        row.referenceFileId(),
                        row.fileName(),
                        row.processingStatus()
                ))
                .toList();

        List<CitationResult> citations = detailQueryPort
                .findCitations(query.reviewId())
                .stream()
                .map(row -> new CitationResult(
                        row.rankOrder(),
                        row.documentRole(),
                        row.bidAttachmentId(),
                        row.referenceFileId(),
                        row.fileName(),
                        row.pageNumber(),
                        row.sheetName(),
                        row.excerpt()
                ))
                .toList();

        return new BidReviewDetailResult(
                review.reviewId(),
                review.noticeId(),
                review.prompt(),
                review.reviewStatus(),
                review.result(),
                review.errorMessage(),
                review.requestedAt(),
                review.completedAt(),
                review.expiresAt(),
                review.projectId(),
                documents,
                citations
        );
    }
}
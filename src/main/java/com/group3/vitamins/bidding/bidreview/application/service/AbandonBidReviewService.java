package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.command.AbandonBidReviewCommand;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCommandPort;
import com.group3.vitamins.bidding.bidreview.application.result.AbandonBidReviewResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.AbandonBidReviewUseCase;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.repository.BidReviewRepository;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class AbandonBidReviewService implements AbandonBidReviewUseCase {

    private final BidReviewRepository bidReviewRepository;
    private final BidReviewCommandPort bidReviewCommandPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    @Override
    public AbandonBidReviewResult abandon(AbandonBidReviewCommand command) {
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        BidReview review = bidReviewRepository.findById(command.reviewId())
                .orElseThrow(() -> new NotFoundException(
                        BidReviewErrorCode.BIDDING_REVIEW_NOT_FOUND
                ));

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        if (!review.companyId().equals(companyId)
                || !review.requestedBy().equals(command.userId())) {
            throw new ForbiddenException(
                    BidReviewErrorCode.BIDDING_REVIEW_ACCESS_DENIED
            );
        }

        BidReview saved;
        try {
            saved = bidReviewCommandPort.saveAbandonedWithCleanupOutbox(
                    command.reviewId(), LocalDateTime.now(clock)
            );
        } catch (IllegalStateException exception) {
            throw new ConflictException(
                    BidReviewErrorCode.BIDDING_REVIEW_NOT_ABANDONABLE
            );
        }

        return AbandonBidReviewResult.from(saved);
    }
}

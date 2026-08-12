package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface BidReviewJpaRepository
        extends JpaRepository<BidReviewJpaEntity, Long> {

    Optional<BidReviewJpaEntity>
    findByReviewIdAndCompanyIdAndRequestedBy(
            Long reviewId,
            Long companyId,
            String requestedBy
    );

    Optional<BidReviewJpaEntity>
    findByReviewIdAndProcessingAttemptId(
            Long reviewId,
            String processingAttemptId
    );

    boolean existsByCompanyIdAndNoticeIdAndRequestedByAndReviewStatusIn(
            Long companyId,
            Long noticeId,
            String requestedBy,
            Collection<BidReviewStatus> reviewStatuses
    );
}
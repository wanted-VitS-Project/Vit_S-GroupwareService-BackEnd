package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewExpiryScanPort;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaBidReviewExpiryScanAdapter
        implements BidReviewExpiryScanPort {

    private static final String CLEANUP_REQUESTED_EVENT =
            "BID_REVIEW_CLEANUP_REQUESTED";

    private final BidReviewJpaRepository reviewRepository;
    private final BidReviewOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Long> findExpiredCandidateIds(LocalDateTime now, int batchSize) {
        return reviewRepository.findExpiredCandidateIds(now, batchSize);
    }

    @Override
    @Transactional
    public boolean claimAndRequestCleanup(Long reviewId, LocalDateTime now) {
        Optional<BidReviewJpaEntity> found = reviewRepository.findForWorkerUpdate(reviewId);
        if (found.isEmpty()) {
            return false;
        }

        BidReviewJpaEntity entity = found.get();
        BidReview review = entity.toDomain();

        if (entity.getCleanupStartedAt() != null
                || review.projectId() != null
                || !isExpiryEligible(review.reviewStatus())
                || review.expiresAt() == null
                || now.isBefore(review.expiresAt())) {
            return false;
        }

        entity.apply(review.markCleanupStarted(now));

        JsonNode payload = objectMapper.createObjectNode()
                .put("reviewId", entity.getReviewId())
                .put("companyId", entity.getCompanyId());

        outboxRepository.save(BidReviewOutboxJpaEntity.pending(
                UUID.randomUUID().toString(),
                entity.getReviewId(),
                "expire-" + entity.getReviewId(),
                CLEANUP_REQUESTED_EVENT,
                payload,
                now
        ));

        return true;
    }

    private boolean isExpiryEligible(BidReviewStatus status) {
        return status == BidReviewStatus.COMPLETED
                || status == BidReviewStatus.FAILED;
    }
}
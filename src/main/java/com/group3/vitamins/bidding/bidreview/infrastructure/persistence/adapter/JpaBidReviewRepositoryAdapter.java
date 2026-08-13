package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.domain.repository.BidReviewRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewDocumentJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaBidReviewRepositoryAdapter
        implements BidReviewRepository {

    private static final List<BidReviewStatus> PROCESSING_STATUSES =
            List.of(
                    BidReviewStatus.PENDING,
                    BidReviewStatus.PROCESSING
            );

    private final BidReviewJpaRepository reviewRepository;
    private final BidReviewDocumentJpaRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<BidReview> findById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(BidReviewJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidReviewDocument> findDocumentsByReviewId(Long reviewId) {
        return documentRepository
                .findAllByReviewIdOrderByDocumentRoleAscReviewDocumentIdAsc(reviewId)
                .stream()
                .map(BidReviewDocumentJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsProcessingReview(
            Long companyId,
            Long noticeId,
            String requestedBy
    ) {
        return reviewRepository
                .existsByCompanyIdAndNoticeIdAndRequestedByAndReviewStatusIn(
                        companyId,
                        noticeId,
                        requestedBy,
                        PROCESSING_STATUSES
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BidReview> findByIdAndAttemptId(
            Long reviewId,
            String attemptId
    ) {
        return reviewRepository
                .findByReviewIdAndProcessingAttemptId(
                        reviewId,
                        attemptId
                )
                .map(BidReviewJpaEntity::toDomain);
    }
}
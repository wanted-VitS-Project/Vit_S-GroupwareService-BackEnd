package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCommandPort;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewDocumentJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaBidReviewCommandAdapter
        implements BidReviewCommandPort {

    private static final String REVIEW_REQUESTED_EVENT =
            "BID_REVIEW_REQUESTED";
    private static final String REVIEW_CLEANUP_REQUESTED_EVENT =
            "BID_REVIEW_CLEANUP_REQUESTED";

    private static final List<BidReviewStatus> PROCESSING_STATUSES =
            List.of(
                    BidReviewStatus.PENDING,
                    BidReviewStatus.PROCESSING
            );

    private final BidReviewJpaRepository reviewRepository;
    private final BidReviewDocumentJpaRepository documentRepository;
    private final BidReviewOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsProcessing(
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
    @Transactional
    public BidReview savePendingWithDocumentsAndOutbox(
            BidReview review,
            List<BidReviewDocument> documents
    ) {
        BidReviewJpaEntity savedReview = reviewRepository.saveAndFlush(
                BidReviewJpaEntity.from(review)
        );

        List<BidReviewDocumentJpaEntity> documentEntities =
                documents.stream()
                        .map(document ->
                                BidReviewDocumentJpaEntity.from(
                                        savedReview.getReviewId(),
                                        document
                                )
                        )
                        .toList();

        documentRepository.saveAll(documentEntities);

        JsonNode payload = objectMapper.createObjectNode()
                .put("reviewId", savedReview.getReviewId())
                .put("companyId", savedReview.getCompanyId())
                .put("attemptId", savedReview.getProcessingAttemptId())
                .put("retryCount", savedReview.getRetryCount());

        BidReviewOutboxJpaEntity outbox =
                BidReviewOutboxJpaEntity.pending(
                        UUID.randomUUID().toString(),
                        savedReview.getReviewId(),
                        savedReview.getProcessingAttemptId(),
                        REVIEW_REQUESTED_EVENT,
                        payload,
                        savedReview.getCreatedAt()
                );

        outboxRepository.save(outbox);
        return savedReview.toDomain();
    }

    @Override
    @Transactional
    public BidReview saveAbandonedWithCleanupOutbox(BidReview review) {
        BidReviewJpaEntity entity = reviewRepository.findById(review.reviewId())
                .orElseThrow(() -> new IllegalStateException(
                        "입찰 문서 검토를 찾을 수 없습니다."
                ));
        entity.apply(review);
        BidReviewJpaEntity savedReview = reviewRepository.saveAndFlush(entity);

        JsonNode payload = objectMapper.createObjectNode()
                .put("reviewId", savedReview.getReviewId())
                .put("companyId", savedReview.getCompanyId());

        BidReviewOutboxJpaEntity outbox =
                BidReviewOutboxJpaEntity.pending(
                        UUID.randomUUID().toString(),
                        savedReview.getReviewId(),
                        UUID.randomUUID().toString(),
                        REVIEW_CLEANUP_REQUESTED_EVENT,
                        payload,
                        savedReview.getUpdatedAt()
                );

        outboxRepository.save(outbox);
        return savedReview.toDomain();
    }
}
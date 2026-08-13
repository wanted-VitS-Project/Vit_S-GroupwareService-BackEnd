package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCleanupStorePort;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocumentRole;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewDocumentJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaBidReviewCleanupStoreAdapter
        implements BidReviewCleanupStorePort {

    private static final String CLEANUP_FAILED_ERROR = "S3_CLEANUP_FAILED";

    private final BidReviewOutboxJpaRepository outboxRepository;
    private final BidReviewJpaRepository reviewRepository;
    private final BidReviewDocumentJpaRepository documentRepository;
    private final FileStoragePort fileStoragePort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Optional<Long> claimNext(
            String lockOwner,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    ) {
        outboxRepository.markExhaustedAsFailed(now);

        List<Long> ids = outboxRepository.findCleanupPublishableIdsForUpdate(now, 1);
        if (ids.isEmpty()) {
            return Optional.empty();
        }

        Long outboxId = ids.get(0);
        outboxRepository.findById(outboxId)
                .ifPresent(outbox -> outbox.claim(lockOwner, lockExpiresAt, now));

        return Optional.of(outboxId);
    }

    @Override
    @Transactional
    public void execute(Long outboxId, String lockOwner, LocalDateTime now) {
        BidReviewOutboxJpaEntity outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("정리 Outbox를 찾을 수 없습니다."));

        CleanupPayload payload = objectMapper.convertValue(
                outbox.getPayload(), CleanupPayload.class
        );

        List<BidReviewDocumentJpaEntity> documents = documentRepository
                .findAllByReviewIdAndDocumentRoleAndDeletedAtIsNull(
                        payload.reviewId(), BidReviewDocumentRole.BID_ATTACHMENT
                );

        List<String> storageKeys = documents.stream()
                .map(BidReviewDocumentJpaEntity::getTemporaryStorageKey)
                .filter(Objects::nonNull)
                .toList();

        if (!storageKeys.isEmpty()) {
            fileStoragePort.deleteObjects(storageKeys);
        }

        documents.forEach(document ->
                document.apply(document.toDomain().cleanup(now)));

        BidReviewJpaEntity review = reviewRepository.findForWorkerUpdate(payload.reviewId())
                .orElseThrow(() -> new IllegalStateException("입찰 문서 검토를 찾을 수 없습니다."));
        review.apply(review.toDomain().expire(now));

        if (!outbox.markPublished(lockOwner, now)) {
            throw new IllegalStateException("정리 Outbox 완료 상태 전이에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public void markFailed(
            Long outboxId,
            String lockOwner,
            LocalDateTime nextAvailableAt,
            LocalDateTime now
    ) {
        outboxRepository.findById(outboxId).ifPresent(outbox ->
                outbox.markPublishFailed(lockOwner, CLEANUP_FAILED_ERROR, nextAvailableAt, now));
    }

    private record CleanupPayload(Long reviewId, Long companyId) {
        private CleanupPayload {
            Objects.requireNonNull(reviewId, "검토 ID는 필수입니다.");
            Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
        }
    }
}
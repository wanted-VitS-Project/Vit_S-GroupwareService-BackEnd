package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.application.model.ClaimedBidReviewOutbox;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewOutboxStorePort;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JpaBidReviewOutboxStoreAdapter
        implements BidReviewOutboxStorePort {

    private final BidReviewOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ClaimedBidReviewOutbox> claimPublishable(
            String lockOwner,
            int batchSize,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    ) {
        outboxRepository.markExhaustedAsFailed(now);

        List<Long> ids = outboxRepository.findPublishableIdsForUpdate(now, batchSize);

        List<ClaimedBidReviewOutbox> claimed = new ArrayList<>();

        for (BidReviewOutboxJpaEntity outbox : outboxRepository.findAllById(ids)) {
            outbox.claim(lockOwner, lockExpiresAt, now);

            try {
                claimed.add(toClaimedOutbox(outbox));
            } catch (IllegalArgumentException exception) {
                outbox.markInvalidPayload(now);
            }
        }

        return claimed;
    }

    @Override
    @Transactional
    public void markPublished(Long outboxId, String lockOwner, LocalDateTime publishedAt) {
        BidReviewOutboxJpaEntity outbox = findRequired(outboxId);

        if (!outbox.markPublished(lockOwner, publishedAt)) {
            throw new IllegalStateException("입찰 문서 검토 Outbox 발행 완료 상태 전이에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public void markPublishFailed(
            Long outboxId,
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt,
            LocalDateTime failedAt
    ) {
        BidReviewOutboxJpaEntity outbox = findRequired(outboxId);

        if (!outbox.markPublishFailed(lockOwner, errorMessage, nextAvailableAt, failedAt)) {
            throw new IllegalStateException("입찰 문서 검토 Outbox 발행 실패 상태 전이에 실패했습니다.");
        }
    }

    private ClaimedBidReviewOutbox toClaimedOutbox(BidReviewOutboxJpaEntity outbox) {
        ReviewJobPayload payload = objectMapper.convertValue(
                outbox.getPayload(), ReviewJobPayload.class
        );

        return new ClaimedBidReviewOutbox(
                outbox.getOutboxId(),
                outbox.getEventId(),
                outbox.getEventType(),
                payload.reviewId(),
                payload.companyId(),
                payload.attemptId(),
                payload.retryCount(),
                outbox.getPublishAttemptCount()
        );
    }

    private BidReviewOutboxJpaEntity findRequired(Long outboxId) {
        return outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("입찰 문서 검토 Outbox를 찾을 수 없습니다."));
    }

    private record ReviewJobPayload(
            Long reviewId,
            Long companyId,
            String attemptId,
            int retryCount
    ) {
        private ReviewJobPayload {
            Objects.requireNonNull(reviewId, "검토 ID는 필수입니다.");
            Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
            Objects.requireNonNull(attemptId, "처리 시도 ID는 필수입니다.");

            if (retryCount < 0) {
                throw new IllegalArgumentException("재시도 횟수는 0 이상이어야 합니다.");
            }
        }
    }
}
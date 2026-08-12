package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidsummary.application.model.ClaimedBidNoticeSummaryOutbox;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryOutboxStorePort;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryOutboxJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryOutboxJpaRepository;
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
public class JpaBidNoticeSummaryOutboxStoreAdapter
        implements BidNoticeSummaryOutboxStorePort {

    private final BidNoticeSummaryOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ClaimedBidNoticeSummaryOutbox> claimPublishable(
            String lockOwner,
            int batchSize,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    ) {
        outboxRepository.markExhaustedAsFailed(now);

        List<Long> ids = outboxRepository.findPublishableIdsForUpdate(
                now,
                batchSize
        );

        List<ClaimedBidNoticeSummaryOutbox> claimed = new ArrayList<>();

        for (BidNoticeSummaryOutboxJpaEntity outbox
                : outboxRepository.findAllById(ids)) {
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
    public void markPublished(
            Long outboxId,
            String lockOwner,
            LocalDateTime publishedAt
    ) {
        BidNoticeSummaryOutboxJpaEntity outbox = findRequired(outboxId);

        if (!outbox.markPublished(lockOwner, publishedAt)) {
            throw new IllegalStateException(
                    "입찰 AI 요약 Outbox 발행 완료 상태 전이에 실패했습니다."
            );
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
        BidNoticeSummaryOutboxJpaEntity outbox = findRequired(outboxId);

        if (!outbox.markPublishFailed(
                lockOwner,
                errorMessage,
                nextAvailableAt,
                failedAt
        )) {
            throw new IllegalStateException(
                    "입찰 AI 요약 Outbox 발행 실패 상태 전이에 실패했습니다."
            );
        }
    }

    private ClaimedBidNoticeSummaryOutbox toClaimedOutbox(
            BidNoticeSummaryOutboxJpaEntity outbox
    ) {
        SummaryJobPayload payload = objectMapper.convertValue(
                outbox.getPayload(),
                SummaryJobPayload.class
        );

        return new ClaimedBidNoticeSummaryOutbox(
                outbox.getOutboxId(),
                outbox.getEventId(),
                outbox.getEventType(),
                payload.summaryId(),
                payload.companyId(),
                payload.attemptId(),
                payload.retryCount(),
                outbox.getPublishAttemptCount()
        );
    }

    private BidNoticeSummaryOutboxJpaEntity findRequired(Long outboxId) {
        return outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException(
                        "입찰 AI 요약 Outbox를 찾을 수 없습니다."
                ));
    }

    private record SummaryJobPayload(
            Long summaryId,
            Long companyId,
            String attemptId,
            int retryCount
    ) {
        private SummaryJobPayload {
            Objects.requireNonNull(summaryId);
            Objects.requireNonNull(companyId);
            Objects.requireNonNull(attemptId);

            if (retryCount < 0) {
                throw new IllegalArgumentException(
                        "재시도 횟수는 0 이상이어야 합니다."
                );
            }
        }
    }
}
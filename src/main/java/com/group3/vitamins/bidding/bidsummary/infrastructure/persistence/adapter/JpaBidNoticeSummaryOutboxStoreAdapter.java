package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidsummary.application.model.ClaimedBidNoticeSummaryOutbox;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryOutboxStorePort;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryOutboxJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryJpaRepository;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaBidNoticeSummaryOutboxStoreAdapter
        implements BidNoticeSummaryOutboxStorePort {

    private static final String INVALID_PAYLOAD_FAILURE_MESSAGE =
            "요약 작업 발행 데이터가 손상되어 처리할 수 없습니다.";
    private static final String PUBLISH_RETRY_EXHAUSTED_MESSAGE =
            "요약 작업을 Redis에 반복 발행하지 못해 처리를 중단합니다.";

    private final BidNoticeSummaryOutboxJpaRepository outboxRepository;
    private final BidNoticeSummaryJpaRepository summaryRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ClaimedBidNoticeSummaryOutbox> claimPublishable(
            String lockOwner,
            int batchSize,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    ) {
        // markExhaustedAsFailed는 벌크 네이티브 UPDATE라 어떤 행이 바뀌었는지 알려주지 않는다 -
        // 같은 조건으로 summaryId를 먼저 읽어 둬야 FAILED 전이를 요약 쪽에도 반영할 수 있다.
        List<Long> exhaustedSummaryIds = outboxRepository.findExhaustedSummaryIds(now);
        int exhaustedCount = outboxRepository.markExhaustedAsFailed(now);
        if (exhaustedCount > 0) {
            log.error(
                    "Bidding summary outbox publish retries exhausted. exhaustedCount={}, summaryIds={}",
                    exhaustedCount, exhaustedSummaryIds
            );
            exhaustedSummaryIds.forEach(summaryId ->
                    propagateFailureToSummary(summaryId, PUBLISH_RETRY_EXHAUSTED_MESSAGE, now));
        }

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
                log.error(
                        "Bidding summary outbox payload invalid. outboxId={}, summaryId={}, payload={}",
                        outbox.getOutboxId(), outbox.getSummaryId(), outbox.getPayload(),
                        exception
                );
                propagateFailureToSummary(outbox.getSummaryId(), INVALID_PAYLOAD_FAILURE_MESSAGE, now);
            }
        }

        // ⚠️ 후보가 0건이면 로그를 남기지 않는다 - 정상적인 유휴 상태이며, 1초 주기 스케줄러라 매번
        // 남기면 로그가 폭주한다. 있었는데 아무것도 못 남겼는지를 추적하려면 이 로그로 충분하다.
        if (!ids.isEmpty()) {
            log.info(
                    "Bidding summary outbox claim finished. candidateCount={}, claimedCount={}",
                    ids.size(), claimed.size()
            );
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

        if ("FAILED".equals(outbox.getPublishStatus())) {
            log.error(
                    "Bidding summary outbox publish permanently failed. outboxId={}, summaryId={}, publishAttemptCount={}",
                    outbox.getOutboxId(), outbox.getSummaryId(), outbox.getPublishAttemptCount()
            );
            propagateFailureToSummary(outbox.getSummaryId(), PUBLISH_RETRY_EXHAUSTED_MESSAGE, failedAt);
        }
    }

    // Outbox가 최종 FAILED로 종료되면 요약도 함께 FAILED로 전이한다 - 안 그러면 Outbox만 죽고
    // 요약은 PENDING/PROCESSING에 영원히 머문다. 이미 다른 경로로 종료된 요약은 덮어쓰지 않는다.
    private void propagateFailureToSummary(Long summaryId, String errorMessage, LocalDateTime now) {
        summaryRepository.findById(summaryId).ifPresent(summary -> {
            if (summary.getSummaryStatus().isInProgress()) {
                summary.fail(errorMessage, now);
            }
        });
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
package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort.BidNoticeSnapshot;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryWorkerPort;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryOutboxJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryJpaRepository;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaBidNoticeSummaryWorkerAdapter
        implements BidNoticeSummaryWorkerPort {

    private static final String SUMMARY_REQUESTED_EVENT =
            "BIDDING_SUMMARY_REQUESTED";
    private static final int MAX_RETRY_COUNT = 2;
    private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(10);
    private static final Duration SECOND_RETRY_DELAY = Duration.ofSeconds(30);

    private final BidNoticeSummaryJpaRepository repository;
    private final BidNoticeSummaryOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Optional<JobData> claimJob(
            Long summaryId,
            String attemptId,
            LocalDateTime now
    ) {
        return repository.findForWorkerUpdate(summaryId)
                .filter(entity -> entity.canClaim(attemptId))
                .map(entity -> {
                    entity.startProcessing(now);

                    BidNoticeSnapshot notice = objectMapper.convertValue(
                            entity.getNoticeSnapshot(),
                            BidNoticeSnapshot.class
                    );

                    return new JobData(
                            entity.getSummaryId(),
                            entity.getCompanyId(),
                            entity.getProcessingAttemptId(),
                            entity.getPrompt(),
                            notice
                    );
                });
    }

    @Override
    @Transactional
    public CallbackUpdate complete(
            Long summaryId,
            String attemptId,
            CompletedSummary result,
            LocalDateTime now
    ) {
        Optional<BidNoticeSummaryJpaEntity> found =
                repository.findForWorkerUpdate(summaryId);

        if (found.isEmpty()) {
            return new CallbackUpdate(false, false, null);
        }

        BidNoticeSummaryJpaEntity entity = found.get();

        if (!entity.isCurrentProcessingAttempt(attemptId)) {
            return ignored(entity);
        }

        entity.complete(
                result.overviewSummary(),
                result.amountSummary(),
                result.scheduleSummary(),
                result.qualificationSummary(),
                result.taskSummary(),
                result.riskSummary(),
                now
        );

        return accepted(entity);
    }

    @Override
    @Transactional
    public CallbackUpdate fail(
            Long summaryId,
            String attemptId,
            String errorMessage,
            boolean retryable,
            LocalDateTime now
    ) {
        Optional<BidNoticeSummaryJpaEntity> found =
                repository.findForWorkerUpdate(summaryId);

        if (found.isEmpty()) {
            return new CallbackUpdate(false, false, null);
        }

        BidNoticeSummaryJpaEntity entity = found.get();

        if (!entity.isCurrentProcessingAttempt(attemptId)) {
            return ignored(entity);
        }

        if (retryable && entity.getRetryCount() < MAX_RETRY_COUNT) {
            prepareRetry(entity, errorMessage, now);
            return accepted(entity);
        }

        entity.fail(errorMessage, now);
        return accepted(entity);
    }

    // 새 attemptId로 상태를 되돌리고 같은 트랜잭션에 지연 Outbox를 저장합니다.
    private void prepareRetry(
            BidNoticeSummaryJpaEntity entity,
            String errorMessage,
            LocalDateTime now
    ) {
        int nextRetryCount = entity.getRetryCount() + 1;
        String nextAttemptId = UUID.randomUUID().toString();
        LocalDateTime availableAt = now.plus(
                nextRetryCount == 1
                        ? FIRST_RETRY_DELAY
                        : SECOND_RETRY_DELAY
        );

        entity.prepareRetry(nextAttemptId, errorMessage, now);

        outboxRepository.save(
                BidNoticeSummaryOutboxJpaEntity.pending(
                        UUID.randomUUID().toString(),
                        entity.getSummaryId(),
                        nextAttemptId,
                        SUMMARY_REQUESTED_EVENT,
                        createPayload(entity),
                        availableAt,
                        now
                )
        );
    }

    // Redis에는 재시도 작업을 식별하는 최소 정보만 저장합니다.
    private JsonNode createPayload(
            BidNoticeSummaryJpaEntity entity
    ) {
        return objectMapper.createObjectNode()
                .put("summaryId", entity.getSummaryId())
                .put("companyId", entity.getCompanyId())
                .put("attemptId", entity.getProcessingAttemptId())
                .put("retryCount", entity.getRetryCount());
    }

    private CallbackUpdate accepted(BidNoticeSummaryJpaEntity entity) {
        return new CallbackUpdate(
                true,
                true,
                entity.getSummaryStatus()
        );
    }

    private CallbackUpdate ignored(BidNoticeSummaryJpaEntity entity) {
        return new CallbackUpdate(
                true,
                false,
                entity.getSummaryStatus()
        );
    }
}

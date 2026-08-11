package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunFailureType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskFailure;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunJpaEntity;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunOutboxJpaEntity;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository.CollectionRunOutboxJpaRepository;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository.SpringDataCollectionRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JpaCollectionRunOutboxStoreAdapter
        implements CollectionRunOutboxStorePort {

    private static final String TASK_DLQ_EVENT_TYPE =
            "BIDDING_COLLECTION_TASK_DLQ_REQUESTED";

    private final CollectionRunOutboxJpaRepository outboxRepository;
    private final SpringDataCollectionRunRepository runRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    // 신규 Outbox를 Redis 메시지 payload와 함께 저장합니다.
    @Override
    @Transactional
    public void savePending(CollectionRunOutbox.Pending outbox) {
        CollectionRunJpaEntity runEntity =
                runRepository.getReferenceById(outbox.runId());

        CollectionRunJobPayload payload = new CollectionRunJobPayload(
                outbox.runId(),
                outbox.conditionId(),
                outbox.companyId(),
                outbox.attemptId(),
                outbox.retryCount()
        );

        CollectionRunOutboxJpaEntity entity =
                CollectionRunOutboxJpaEntity.pending(
                        outbox.eventId(),
                        runEntity,
                        null,
                        outbox.attemptId(),
                        outbox.eventType(),
                        objectMapper.valueToTree(payload),
                        outbox.createdAt()
                );

        outboxRepository.save(entity);
    }

    // Task 실패와 함께 저장할 DLQ Outbox payload를 생성합니다.
    @Override
    @Transactional
    public void saveTaskFailurePending(
            String eventId,
            CollectionRunTaskFailure failure,
            LocalDateTime createdAt
    ) {
        CollectionRunJpaEntity runEntity =
                runRepository.getReferenceById(failure.runId());
        CollectionRequestCombination target = failure.target();
        CollectionRunTaskFailurePayload payload =
                new CollectionRunTaskFailurePayload(
                        failure.runId(),
                        failure.taskId(),
                        failure.companyId(),
                        failure.attemptId(),
                        failure.retryCount(),
                        failure.failureType().name(),
                        target.noticeType().name(),
                        target.keyword(),
                        target.regionCode(),
                        target.industryCode(),
                        target.pageNumber()
                );

        CollectionRunOutboxJpaEntity entity =
                CollectionRunOutboxJpaEntity.pending(
                        eventId,
                        runEntity,
                        failure.taskId(),
                        failure.attemptId(),
                        TASK_DLQ_EVENT_TYPE,
                        objectMapper.valueToTree(payload),
                        createdAt
                );
        outboxRepository.save(entity);
    }

    // 발행 가능한 Outbox를 잠금 조회하고 현재 서버가 점유합니다.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ClaimedCollectionRunOutbox> claimPublishable(
            String lockOwner,
            int batchSize,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    ) {
        outboxRepository.markExhaustedAsFailed(now);
        List<Long> outboxIds =
                outboxRepository.findPublishableIdsForUpdate(now, batchSize);

        List<ClaimedCollectionRunOutbox> claimedOutboxes = new ArrayList<>();
        for (CollectionRunOutboxJpaEntity outbox
                : outboxRepository.findAllById(outboxIds)) {
            outbox.claim(lockOwner, lockExpiresAt, now);
            try {
                claimedOutboxes.add(toClaimedOutbox(outbox));
            } catch (IllegalArgumentException exception) {
                outbox.markInvalidPayload(now);
            }
        }
        return claimedOutboxes;
    }

    // 현재 서버가 점유한 Outbox의 Redis 발행 완료를 기록합니다.
    @Override
    @Transactional
    public void markPublished(
            Long outboxId,
            String lockOwner,
            LocalDateTime publishedAt
    ) {
        CollectionRunOutboxJpaEntity outbox = findRequired(outboxId);
        if (!outbox.markPublished(lockOwner, publishedAt)) {
            throw new IllegalStateException(
                    "Outbox 발행 완료 상태 전이에 실패했습니다."
            );
        }
    }

    // 발행 실패를 기록하고 다음 재시도 시각까지 점유를 해제합니다.
    @Override
    @Transactional
    public void markPublishFailed(
            Long outboxId,
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt
    ) {
        CollectionRunOutboxJpaEntity outbox = findRequired(outboxId);
        if (!outbox.markPublishFailed(
                lockOwner,
                errorMessage,
                nextAvailableAt,
                LocalDateTime.now(clock)
        )) {
            throw new IllegalStateException(
                    "Outbox 발행 실패 상태 전이에 실패했습니다."
            );
        }
    }

    // DB payload를 Redis Publisher가 사용할 모델로 복원합니다.
    private ClaimedCollectionRunOutbox toClaimedOutbox(
            CollectionRunOutboxJpaEntity outbox
    ) {
        if (TASK_DLQ_EVENT_TYPE.equals(outbox.getEventType())) {
            return toClaimedTaskFailureOutbox(outbox);
        }

        CollectionRunJobPayload payload = objectMapper.convertValue(
                outbox.getPayload(), CollectionRunJobPayload.class);

        return new ClaimedCollectionRunOutbox(
                outbox.getCrawlRunOutboxId(),
                payload.runId(),
                payload.conditionId(),
                payload.companyId(),
                outbox.getEventId(),
                outbox.getEventType(),
                payload.attemptId(),
                payload.retryCount(),
                outbox.getPublishAttemptCount(),
                null
        );
    }

    // Task DLQ payload를 Redis 발행 모델로 복원합니다.
    private ClaimedCollectionRunOutbox toClaimedTaskFailureOutbox(
            CollectionRunOutboxJpaEntity outbox
    ) {
        CollectionRunTaskFailurePayload payload = objectMapper.convertValue(
                outbox.getPayload(), CollectionRunTaskFailurePayload.class);
        CollectionRunTaskFailure failure = new CollectionRunTaskFailure(
                payload.runId(),
                payload.taskId(),
                payload.companyId(),
                payload.attemptId(),
                payload.retryCount(),
                CollectionRunFailureType.valueOf(payload.failureType()),
                new CollectionRequestCombination(
                        BidNoticeType.valueOf(payload.noticeType()),
                        payload.keyword(),
                        payload.regionCode(),
                        payload.industryCode(),
                        payload.pageNumber()
                )
        );
        return new ClaimedCollectionRunOutbox(
                outbox.getCrawlRunOutboxId(),
                payload.runId(),
                null,
                payload.companyId(),
                outbox.getEventId(),
                outbox.getEventType(),
                payload.attemptId(),
                payload.retryCount(),
                outbox.getPublishAttemptCount(),
                failure
        );
    }

    private CollectionRunOutboxJpaEntity findRequired(Long outboxId) {
        return outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox를 찾을 수 없습니다."
                ));
    }

    // Redis Stream에 전달할 최소 작업 데이터 구조입니다.
    private record CollectionRunJobPayload(
            Long runId,
            Long conditionId,
            Long companyId,
            String attemptId,
            int retryCount
    ) {
        private CollectionRunJobPayload {
            Objects.requireNonNull(runId, "수집 실행 ID는 필수입니다.");
            Objects.requireNonNull(conditionId, "수집 조건 ID는 필수입니다.");
            Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
            Objects.requireNonNull(attemptId, "처리 시도 ID는 필수입니다.");
            if (retryCount < 0) {
                throw new IllegalArgumentException(
                        "재시도 횟수는 0 이상이어야 합니다."
                );
            }
        }
    }

    // DB JSON에 저장할 Task 영구 실패 payload입니다.
    private record CollectionRunTaskFailurePayload(
            Long runId,
            Long taskId,
            Long companyId,
            String attemptId,
            int retryCount,
            String failureType,
            String noticeType,
            String keyword,
            String regionCode,
            String industryCode,
            int pageNumber
    ) {
    }
}

package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.filecleanup.application.model.ClaimedVitamateCleanupOutbox;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupOutboxStorePort;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupJobEntity;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupOutboxEntity;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository.VitamateCleanupOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaVitamateCleanupOutboxStoreAdapter
        implements VitamateCleanupOutboxStorePort {

    private final VitamateCleanupOutboxJpaRepository outboxRepository;

    // 발행 가능한 Outbox를 잠근 뒤 현재 서버가 점유합니다.
    @Override
    @Transactional
    public List<ClaimedVitamateCleanupOutbox> claimPublishable(
            String lockOwner,
            int batchSize,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    ) {
        List<Long> outboxIds =
                outboxRepository.findPublishableIdsForUpdate(now, batchSize);

        List<VitamateCleanupOutboxEntity> outboxes =
                outboxRepository.findAllById(outboxIds);

        return outboxes.stream()
                .map(outbox -> {
                    VitamateCleanupJobEntity job = outbox.getCleanupJob();

                    if (job.getCurrentAttemptId() == null) {
                        job.prepareAttempt(
                                UUID.randomUUID().toString(),
                                now
                        );
                    }

                    outbox.claim(lockOwner, lockExpiresAt, now);

                    return toClaimedOutbox(outbox);
                })
                .toList();
    }

    // Redis 발행에 성공한 Outbox를 완료 처리합니다.
    @Override
    @Transactional
    public void markPublished(
            Long outboxId,
            String lockOwner,
            LocalDateTime publishedAt
    ) {
        outboxRepository.findById(outboxId)
                .ifPresent(outbox -> {
                    boolean published =
                            outbox.markPublished(lockOwner, publishedAt);

                    if (published) {
                        outbox.getCleanupJob().markPublished(
                                outbox.getCleanupJob().getCurrentAttemptId(),
                                publishedAt
                        );
                    }
                });
    }

    // 발행 실패 정보를 저장하고 다음 재시도 시각을 지정합니다.
    @Override
    @Transactional
    public void markPublishFailed(
            Long outboxId,
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt
    ) {
        outboxRepository.findById(outboxId)
                .ifPresent(outbox ->
                        outbox.markPublishFailed(
                                lockOwner,
                                errorMessage,
                                nextAvailableAt,
                                LocalDateTime.now()
                        )
                );
    }

    private ClaimedVitamateCleanupOutbox toClaimedOutbox(
            VitamateCleanupOutboxEntity outbox
    ) {
        VitamateCleanupJobEntity job = outbox.getCleanupJob();

        List<Long> fileVersionIds = new ArrayList<>();
        job.getFileVersionIds().forEach(node ->
                fileVersionIds.add(node.asLong())
        );

        return new ClaimedVitamateCleanupOutbox(
                outbox.getCleanupOutboxId(),
                job.getCleanupJobId(),
                outbox.getEventId(),
                outbox.getEventType(),
                job.getCleanupKey(),
                job.getCurrentAttemptId(),
                fileVersionIds,
                Math.max(0, job.getAttemptCount() - 1),
                outbox.getPublishAttemptCount()
        );
    }
}

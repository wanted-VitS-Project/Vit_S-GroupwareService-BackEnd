package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTask;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskSummary;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskPort;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTaskStatus;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunTaskJpaEntity;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository.SpringDataCollectionRunTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaCollectionRunTaskAdapter implements CollectionRunTaskPort {

    private final SpringDataCollectionRunTaskRepository repository;

    // 하나의 실행에 필요한 외부 요청 조합들을 대기 작업으로 저장합니다.
    @Override
    @Transactional
    public void createTasks(Long runId, List<CollectionRequestCombination> combinations) {
        LocalDateTime now = LocalDateTime.now();
        List<CollectionRunTaskJpaEntity> tasks = combinations.stream()
                .distinct()
                .map(target -> CollectionRunTaskJpaEntity.create(runId, target, now))
                .toList();
        repository.saveAll(tasks);
    }

    // 조건부 UPDATE로 대기 작업 또는 lease 만료 작업 하나를 원자적으로 점유합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<CollectionRunTask> claim(
            Long runId,
            CollectionRequestCombination target,
            String attemptId,
            int retryCount,
            LocalDateTime startedAt,
            LocalDateTime leaseExpiresAt
    ) {
        int changed = repository.claimTask(
                runId,
                target.noticeType(),
                normalize(target.keyword()),
                normalize(target.regionCode()),
                normalize(target.industryCode()),
                target.pageNumber(),
                attemptId,
                retryCount,
                startedAt,
                leaseExpiresAt,
                CollectionRunTaskStatus.PENDING,
                CollectionRunTaskStatus.PROCESSING
        );
        if (changed == 0) {
            return Optional.empty();
        }

        return repository.findByCrawlRunIdAndAttemptIdAndTaskStatus(
                        runId,
                        attemptId,
                        CollectionRunTaskStatus.PROCESSING
                )
                .map(this::toModel);
    }

    // 현재 점유 시도와 일치하는 작업만 완료 상태로 변경합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(
            Long taskId,
            String attemptId,
            int collectedCount,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            LocalDateTime finishedAt
    ) {
        return repository.completeTask(
                taskId,
                attemptId,
                collectedCount,
                insertedCount,
                updatedCount,
                skippedCount,
                finishedAt,
                CollectionRunTaskStatus.PROCESSING,
                CollectionRunTaskStatus.COMPLETED
        ) == 1;
    }

    // 재시도할 작업의 점유 정보를 비우고 다시 대기 상태로 전환합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareRetry(
            Long taskId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime updatedAt
    ) {
        return repository.prepareRetry(
                taskId,
                attemptId,
                errorCode,
                errorMessage,
                updatedAt,
                CollectionRunTaskStatus.PENDING,
                CollectionRunTaskStatus.PROCESSING
        ) == 1;
    }

    // 재시도할 수 없는 작업을 최종 실패 상태로 전환합니다.
    @Override
    @Transactional
    public boolean fail(
            Long taskId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime finishedAt
    ) {
        return repository.failTask(
                taskId,
                attemptId,
                errorCode,
                errorMessage,
                finishedAt,
                CollectionRunTaskStatus.PROCESSING,
                CollectionRunTaskStatus.FAILED
        ) == 1;
    }

    // 실행에서 가장 먼저 처리할 대기 작업 또는 lease 만료 작업을 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public Optional<CollectionRunTask> findNextProcessableTask(
            Long runId,
            LocalDateTime now
    ) {
        return repository.findProcessableTasks(
                        runId,
                        now,
                        CollectionRunTaskStatus.PENDING,
                        CollectionRunTaskStatus.PROCESSING,
                        PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .map(this::toModel);
    }

    // 모든 요청 조합의 상태와 처리 건수를 실행 단위로 합산합니다.
    @Override
    @Transactional(readOnly = true)
    public CollectionRunTaskSummary summarize(Long runId) {
        List<CollectionRunTaskJpaEntity> tasks = repository.findAllByCrawlRunId(runId);
        return new CollectionRunTaskSummary(
                tasks.size(),
                countStatus(tasks, CollectionRunTaskStatus.PENDING),
                countStatus(tasks, CollectionRunTaskStatus.PROCESSING),
                countStatus(tasks, CollectionRunTaskStatus.COMPLETED),
                countStatus(tasks, CollectionRunTaskStatus.FAILED),
                tasks.stream().mapToInt(CollectionRunTaskJpaEntity::getCollectedCount).sum(),
                tasks.stream().mapToInt(CollectionRunTaskJpaEntity::getInsertedCount).sum(),
                tasks.stream().mapToInt(CollectionRunTaskJpaEntity::getUpdatedCount).sum(),
                tasks.stream().mapToInt(CollectionRunTaskJpaEntity::getSkippedCount).sum()
        );
    }

    private CollectionRunTask toModel(CollectionRunTaskJpaEntity entity) {
        return new CollectionRunTask(
                entity.getCrawlRunTaskId(),
                entity.getCrawlRunId(),
                new CollectionRequestCombination(
                        entity.getNoticeType(),
                        denormalize(entity.getKeyword()),
                        denormalize(entity.getRegionCode()),
                        denormalize(entity.getIndustryCode()),
                        entity.getPageNumber()
                ),
                entity.getTaskStatus(),
                entity.getAttemptId(),
                entity.getRetryCount(),
                entity.getLeaseExpiresAt()
        );
    }

    private int countStatus(
            List<CollectionRunTaskJpaEntity> tasks,
            CollectionRunTaskStatus status
    ) {
        return (int) tasks.stream()
                .filter(task -> task.getTaskStatus() == status)
                .count();
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    private String denormalize(String value) {
        return value.isBlank() ? null : value;
    }
}

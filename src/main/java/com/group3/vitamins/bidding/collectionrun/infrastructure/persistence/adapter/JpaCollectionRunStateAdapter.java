package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRun;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunStatePort;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper.CollectionRunConditionSnapshotJsonMapper;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository.SpringDataCollectionRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaCollectionRunStateAdapter implements CollectionRunStatePort {

    private final SpringDataCollectionRunRepository repository;
    private final CollectionRunConditionSnapshotJsonMapper snapshotMapper;

    // 대기 중인 실행을 현재 Worker의 처리 시도로 점유합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedCollectionRun> claim(
            Long runId,
            Long companyId,
            String attemptId,
            int retryCount,
            LocalDateTime startedAt,
            LocalDateTime leaseExpiresAt
    ) {
        int changed = repository.claimPendingRun(
                runId,
                companyId,
                attemptId,
                retryCount,
                startedAt,
                leaseExpiresAt,
                CollectionRunStatus.PENDING,
                CollectionRunStatus.PROCESSING
        );
        if (changed == 0) {
            return Optional.empty();
        }

        return repository
                .findByCrawlRunIdAndCrawlCondition_CompanyIdAndProcessingAttemptIdAndRunStatusAndDeletedAtIsNull(
                        runId,
                        companyId,
                        attemptId,
                        CollectionRunStatus.PROCESSING
                )
                .map(run -> new ClaimedCollectionRun(
                        run.getCrawlRunId(),
                        snapshotMapper.fromJson(run.getConditionSnapshot())
                ));
    }

    // 현재 처리 시도가 소유한 실행의 점유 만료 시각을 연장합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean renewLease(
            Long runId,
            String attemptId,
            LocalDateTime leaseExpiresAt,
            LocalDateTime updatedAt
    ) {
        return repository.renewLease(
                runId,
                attemptId,
                leaseExpiresAt,
                updatedAt,
                CollectionRunStatus.PROCESSING
        ) == 1;
    }

    // 현재 처리 시도와 일치할 때만 실행 결과와 최종 상태를 저장합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(
            Long runId,
            String attemptId,
            CollectionRunStatus finalStatus,
            int collectedCount,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            LocalDateTime finishedAt
    ) {
        validateCompletionStatus(finalStatus);
        return repository.completeRun(
                runId,
                attemptId,
                finalStatus,
                collectedCount,
                insertedCount,
                updatedCount,
                skippedCount,
                finishedAt,
                CollectionRunStatus.PROCESSING
        ) == 1;
    }

    // 현재 처리 시도와 일치할 때만 실행을 최종 실패 상태로 변경합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean fail(
            Long runId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime finishedAt
    ) {
        return repository.failRun(
                runId,
                attemptId,
                errorCode,
                errorMessage,
                finishedAt,
                CollectionRunStatus.PROCESSING,
                CollectionRunStatus.FAILED
        ) == 1;
    }

    // 일시적 실패 실행을 다음 Redis 재시도가 다시 점유할 수 있도록 반환합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareRetry(
            Long runId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime updatedAt
    ) {
        return repository.prepareRetry(
                runId,
                attemptId,
                errorCode,
                errorMessage,
                updatedAt,
                CollectionRunStatus.PENDING,
                CollectionRunStatus.PROCESSING
        ) == 1;
    }

    private void validateCompletionStatus(CollectionRunStatus finalStatus) {
        if (finalStatus != CollectionRunStatus.COMPLETED
                && finalStatus != CollectionRunStatus.PARTIAL_SUCCESS) {
            throw new IllegalArgumentException(
                    "수집 실행 완료 상태는 COMPLETED 또는 PARTIAL_SUCCESS여야 합니다."
            );
        }
    }
}

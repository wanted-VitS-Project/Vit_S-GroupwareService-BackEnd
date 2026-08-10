package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRun;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePage;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunFailureType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJob;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJobResult;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTask;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskFailure;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskSummary;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectedBidNoticeStorePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunJobHandlerPort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunStatePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskPort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionSourceCollectorPort;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollectionRunJobHandlerService implements CollectionRunJobHandlerPort {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_RETRY_COUNT = 3;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);

    private final CollectionRunStatePort runStatePort;
    private final CollectionRunTaskPort taskPort;
    private final CollectionRunTaskFailureService taskFailureService;
    private final List<CollectionSourceCollectorPort> collectors;
    private final CollectedBidNoticeStorePort noticeStorePort;
    private final Clock clock;

    public CollectionRunJobHandlerService(
            CollectionRunStatePort runStatePort,
            CollectionRunTaskPort taskPort,
            CollectionRunTaskFailureService taskFailureService,
            List<CollectionSourceCollectorPort> collectors,
            CollectedBidNoticeStorePort noticeStorePort,
            Clock clock
    ) {
        this.runStatePort = runStatePort;
        this.taskPort = taskPort;
        this.taskFailureService = taskFailureService;
        this.collectors = collectors;
        this.noticeStorePort = noticeStorePort;
        this.clock = clock;
    }

    // 수집 실행을 점유하고 요청 조합별 외부 수집과 저장, 상태 전이를 순서대로 처리합니다.
    @Override
    public CollectionRunJobResult handle(CollectionRunJob job) {
        LocalDateTime now = LocalDateTime.now(clock);
        ClaimedCollectionRun run = runStatePort.claim(
                        job.runId(),
                        job.companyId(),
                        job.attemptId(),
                        job.retryCount(),
                        now,
                        now.plus(LEASE_DURATION)
                )
                .orElse(null);

        if (run == null) {
            return CollectionRunJobResult.success();
        }

        CollectionRequestCombination retryTarget = job.retryTarget();
        while (true) {
            CollectionRunTask candidate = retryTarget == null
                    ? taskPort.findNextProcessableTask(job.runId(), LocalDateTime.now(clock))
                    .orElse(null)
                    : null;
            CollectionRequestCombination target = retryTarget != null
                    ? retryTarget
                    : candidate == null ? null : candidate.target();
            int taskRetryCount = candidate == null
                    ? job.retryCount()
                    : candidate.retryCount();
            retryTarget = null;

            if (target == null) {
                return finishRun(job);
            }

            CollectionRunTask task = taskPort.claim(
                            job.runId(),
                            target,
                            job.attemptId(),
                            taskRetryCount,
                            LocalDateTime.now(clock),
                            LocalDateTime.now(clock).plus(LEASE_DURATION)
                    )
                    .orElse(null);
            if (task == null) {
                continue;
            }

            CollectionSourceCollectorPort collector = findCollector(
                    run.conditionSnapshot().sourceCode()
            );
            CollectedBidNoticePage page = collector.collect(
                    run.conditionSnapshot(),
                    target,
                    PAGE_SIZE
            );

            if (!page.failures().isEmpty()) {
                CollectionRunJobResult failureResult = handleFailure(
                        job,
                        task,
                        page.failures().get(0)
                );
                if (failureResult != null) {
                    return failureResult;
                }
                continue;
            }

            CollectedBidNoticeStorePort.StoreResult stored = noticeStorePort.saveAll(
                    job.companyId(),
                    run.conditionSnapshot().sourceCode(),
                    job.runId(),
                    page.notices(),
                    LocalDateTime.now(clock)
            );

            if (page.hasNext()) {
                taskPort.createTasks(job.runId(), List.of(nextPage(target)));
            }

            taskPort.complete(
                    task.taskId(),
                    job.attemptId(),
                    page.notices().size(),
                    stored.insertedCount(),
                    stored.updatedCount(),
                    stored.skippedCount(),
                    LocalDateTime.now(clock)
            );
        }
    }

    private CollectionRunJobResult handleFailure(
            CollectionRunJob job,
            CollectionRunTask task,
            CollectedBidNoticePage.CollectionFailure failure
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
        String errorCode = failure.failureType().name();

        if (failure.retryable() && task.retryCount() < MAX_RETRY_COUNT) {
            taskPort.prepareRetry(task.taskId(), job.attemptId(), errorCode, errorCode, now);
            runStatePort.prepareRetry(job.runId(), job.attemptId(), errorCode, errorCode, now);
            return CollectionRunJobResult.retryableFailure(
                    failure.failureType(),
                    task.target()
            );
        }

        taskFailureService.recordPermanentFailure(new CollectionRunTaskFailure(
                job.runId(),
                task.taskId(),
                job.companyId(),
                job.attemptId(),
                task.retryCount(),
                failure.failureType(),
                task.target()
        ), errorCode, errorCode, now);
        return null;
    }

    private CollectionRunJobResult finishRun(CollectionRunJob job) {
        CollectionRunTaskSummary summary = taskPort.summarize(job.runId());
        LocalDateTime now = LocalDateTime.now(clock);

        if (!summary.isFinished()) {
            runStatePort.prepareRetry(
                    job.runId(), job.attemptId(),
                    CollectionRunFailureType.UNKNOWN_PROCESSING_ERROR.name(),
                    "unfinished_collection_tasks", now
            );
            return CollectionRunJobResult.retryableFailure(
                    CollectionRunFailureType.UNKNOWN_PROCESSING_ERROR,
                    null
            );
        }

        if (summary.failedCount() == summary.totalCount()) {
            runStatePort.fail(
                    job.runId(), job.attemptId(),
                    CollectionRunFailureType.UNKNOWN_PROCESSING_ERROR.name(),
                    "all_collection_tasks_failed", now
            );
            return CollectionRunJobResult.success();
        }

        CollectionRunStatus finalStatus = summary.failedCount() > 0
                ? CollectionRunStatus.PARTIAL_SUCCESS
                : CollectionRunStatus.COMPLETED;
        runStatePort.complete(
                job.runId(), job.attemptId(), finalStatus,
                summary.collectedCount(), summary.insertedCount(),
                summary.updatedCount(), summary.skippedCount(), now
        );
        return CollectionRunJobResult.success();
    }

    private CollectionSourceCollectorPort findCollector(String sourceCode) {
        return collectors.stream()
                .filter(collector -> collector.supportedSourceCode().equals(sourceCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 입찰 공고 수집처입니다: " + sourceCode
                ));
    }

    private CollectionRequestCombination nextPage(CollectionRequestCombination target) {
        return new CollectionRequestCombination(
                target.noticeType(),
                target.keyword(),
                target.regionCode(),
                target.industryCode(),
                target.pageNumber() + 1
        );
    }
}

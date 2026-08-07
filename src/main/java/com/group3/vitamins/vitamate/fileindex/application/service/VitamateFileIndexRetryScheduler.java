package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.vitamate.fileindex.application.command.DispatchVitamateFileIndexCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.usecase.DispatchVitamateFileIndexUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// dispatch 트랜잭션은 커밋됐지만 afterCommit 발행 전에 프로세스가 죽어(예: 배포 중 종료)
// file_index가 PENDING으로 멈춘 경우를 찾아 재발행한다. 완전한 트랜잭셔널 outbox는 아니지만,
// "메시지가 조용히 사라짐" 문제를 주기적 재시도로 자가치유한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class VitamateFileIndexRetryScheduler {

    // 정상 처리는 보통 수 초 안에 PROCESSING으로 넘어가므로, 이 시간을 넘겨도 PENDING이면
    // 발행이 안 된 것으로 간주한다.
    private static final long STALE_MINUTES = 5;

    // 한 번에 재발행할 최대 건수 — backlog가 커져도 후보 목록과 로그가 무한히 늘지 않게 한다.
    private static final int BATCH_LIMIT = 100;

    private final VitamateFileIndexStorePort fileIndexStorePort;
    private final DispatchVitamateFileIndexUseCase dispatchUseCase;

    @Scheduled(fixedDelayString = "${vitamate.file-index.retry.fixed-delay-ms:300000}")
    public void retryStalePendingJobs() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        List<Long> candidates = fileIndexStorePort.findStalePendingFileVersionIdCandidates(before, BATCH_LIMIT);

        if (candidates.isEmpty()) {
            return;
        }

        int claimedCount = 0;
        for (Long fileVersionId : candidates) {
            // 후보 조회와 재발행 사이에 worker가 이미 PROCESSING/COMPLETED로 넘겼을 수 있으므로,
            // PENDING·stale 조건을 다시 검사하며 원자적으로 선점한 것만 재발행한다.
            if (!fileIndexStorePort.claimStalePending(fileVersionId, before)) {
                continue;
            }
            claimedCount++;
            dispatchUseCase.handle(new DispatchVitamateFileIndexCommand(fileVersionId));
        }

        if (claimedCount > 0) {
            log.warn("Vitamate file index retry republished stale PENDING jobs. candidateCount={}, claimedCount={}",
                    candidates.size(), claimedCount);
        }
    }
}

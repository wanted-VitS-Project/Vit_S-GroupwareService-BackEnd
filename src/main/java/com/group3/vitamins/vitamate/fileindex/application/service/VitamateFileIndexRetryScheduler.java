package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexJobPublisherPort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort.ReclaimResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// PENDING/PROCESSING인데 lease가 실제로 만료된(=워커가 살아있다는 증거가 사라진) file_index를
// 찾아 재발행하거나, 재시도 상한을 소진했으면 최종 실패로 종료한다.
// ⚠️ 예전에는 updated_at 경과 시간만으로 "유실"을 판단해서, 아직 살아있는 워커(예: Gemini 429
// 백오프로 처리가 오래 걸리는 경우)의 시도를 빼앗아 attemptId를 재발급하는 레이스가 있었다.
// 지금은 dispatch·PROCESSING 확인 시점에 명시적으로 박아둔 lease_expires_at이 실제로 지나야만
// 재claim이 되므로, 살아있는 워커는 절대 이 스케줄러에 의해 attemptId를 빼앗기지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class VitamateFileIndexRetryScheduler {

    // 한 번에 처리할 최대 건수 — backlog가 커져도 후보 목록과 로그가 무한히 늘지 않게 한다.
    private static final int BATCH_LIMIT = 100;

    private final VitamateFileIndexStorePort fileIndexStorePort;
    private final VitamateFileIndexJobPublisherPort jobPublisherPort;

    @Scheduled(fixedDelayString = "${vitamate.file-index.retry.fixed-delay-ms:300000}")
    public void retryStalePendingJobs() {
        LocalDateTime now = LocalDateTime.now();
        failExhaustedJobs(now);
        reclaimRetryableJobs(now);
    }

    // 재시도 상한을 이미 소진한 채로 lease가 만료된 행 — 더 재발행하지 않고 종료 확정한다.
    private void failExhaustedJobs(LocalDateTime now) {
        List<Long> candidates = fileIndexStorePort.findExhaustedFileVersionIdCandidates(now, BATCH_LIMIT);
        if (candidates.isEmpty()) {
            return;
        }

        int failedCount = 0;
        for (Long fileVersionId : candidates) {
            if (fileIndexStorePort.failExhausted(fileVersionId, now, "재시도 횟수를 초과해 파일 인덱싱을 중단했습니다.")) {
                failedCount++;
            }
        }

        if (failedCount > 0) {
            log.warn("Vitamate file index retry exhausted - candidateCount={}, failedCount={}",
                    candidates.size(), failedCount);
        }
    }

    // lease가 만료됐고 재시도 상한 미만인 행 — 원자적으로 선점한 것만 재발행한다.
    private void reclaimRetryableJobs(LocalDateTime now) {
        List<Long> candidates = fileIndexStorePort.findReclaimableFileVersionIdCandidates(now, BATCH_LIMIT);
        if (candidates.isEmpty()) {
            return;
        }

        int claimedCount = 0;
        for (Long fileVersionId : candidates) {
            // 후보 조회와 claim 사이에 워커가 이미 완료했거나 다른 경로로 상태가 바뀌었을 수
            // 있으므로, 재발행 직전에 lease·재시도 상한 조건을 다시 검사하며 원자적으로 선점한다.
            ReclaimResult reclaim = fileIndexStorePort.claimForRetry(fileVersionId, now);
            if (!reclaim.claimed()) {
                continue;
            }
            claimedCount++;
            jobPublisherPort.publish(new VitamateFileIndexJobPublisherPort.FileIndexJob(fileVersionId, 0, now));
        }

        if (claimedCount > 0) {
            log.warn("Vitamate file index retry republished lease-expired jobs. candidateCount={}, claimedCount={}",
                    candidates.size(), claimedCount);
        }
    }
}

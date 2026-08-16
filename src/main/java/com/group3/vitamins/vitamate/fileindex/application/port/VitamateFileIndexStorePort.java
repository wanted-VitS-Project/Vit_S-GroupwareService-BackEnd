package com.group3.vitamins.vitamate.fileindex.application.port;

import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;

import java.time.LocalDateTime;
import java.util.List;

// file_index 테이블 저장과 갱신을 담당하는 포트
public interface VitamateFileIndexStorePort {

    boolean existsFileVersion(Long fileVersionId);

    // PENDING/PROCESSING인데 lease가 실제로 만료됐고(=워커가 살아있다는 증거가 없고) 재시도
    // 상한 미만인 파일 버전 ID 후보를 제한된 개수만 찾는다(재시도 스케줄러용). 후보일 뿐이며
    // 재발행 전 claimForRetry로 다시 확인해야 한다.
    List<Long> findReclaimableFileVersionIdCandidates(LocalDateTime now, int limit);

    // lease가 만료됐지만 이미 재시도 상한을 소진한 파일 버전 ID 후보를 찾는다 — 재발행하지 않고
    // 바로 종료 처리해야 한다.
    List<Long> findExhaustedFileVersionIdCandidates(LocalDateTime now, int limit);

    // 후보 하나를 재확인하며 원자적으로 선점하고 새 attemptId를 발급한다. claimed=false면 그 사이
    // 워커가 이미 완료했거나 다른 경로로 상태가 바뀐 것이므로 재발행하면 안 된다.
    ReclaimResult claimForRetry(Long fileVersionId, LocalDateTime now);

    // lease가 만료되고 재시도 상한도 소진한 행을 최종 실패로 종료한다. true면 이 호출이 실제로
    // 종료시킨 것이고, false면 그 사이 다른 경로로 상태가 이미 바뀐 것이다.
    boolean failExhausted(Long fileVersionId, LocalDateTime now, String errorMessage);

    FileIndexStatusUpdateResult upsertStatus(
            Long fileVersionId,
            String indexAttemptId,
            FileIndexStatus indexStatus,
            String errorMessage,
            boolean retryable,
            LocalDateTime now
    );

    // 파일 인덱싱 상태 저장 결과입니다. attempt가 어긋나면 accepted=false로 늦은 callback을 무시합니다.
    // requeued=true면 FAILED+retryable+재시도 상한 미만이라 같은 트랜잭션에서 즉시 PENDING으로
    // 되돌리고 새 attemptId를 발급했다는 뜻 — 호출자가 새 시도를 큐에 재발행해야 한다.
    record FileIndexStatusUpdateResult(
            boolean accepted,
            String indexAttemptId,
            FileIndexStatus indexStatus,
            String reason,
            boolean requeued
    ) {
    }

    // 재시도 선점 결과입니다. claimed=false면 재발행하면 안 됩니다.
    record ReclaimResult(boolean claimed, String newAttemptId) {
        public static ReclaimResult notClaimed() {
            return new ReclaimResult(false, null);
        }
    }
}

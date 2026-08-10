package com.group3.vitamins.vitamate.filecleanup.application.port;

import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupJob;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VitamateCleanupJobStorePort {

    // 파일 버전 목록을 확보하고 cleanup job과 outbox를 함께 저장합니다.
    void createCleanupJob(Long fileId);

    // 현재 시도의 작업을 처리 중 상태로 변경합니다.
    boolean markProcessing(
            Long cleanupJobId,
            String attemptId,
            LocalDateTime processingStartedAt
    );

    // 현재 시도의 작업을 완료 상태로 변경합니다.
    boolean markCompleted(
            Long cleanupJobId,
            String attemptId,
            int deletedVectorCount,
            LocalDateTime completedAt
    );

    // 재시도할 수 없는 실패를 DLQ 상태로 변경합니다.
    boolean markDeadLetter(
            Long cleanupJobId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime completedAt
    );

    // callback 거부 응답에 사용할 현재 작업 상태를 조회합니다.
    Optional<VitamateCleanupJob.Status> findStatus(Long cleanupJobId);

    // 현재 시도 횟수가 최대 횟수 미만일 때만 재시도를 예약합니다.
    boolean scheduleRetry(
            Long cleanupJobId,
            String attemptId,
            String errorCode,
            String errorMessage,
            int maxAttempts,
            LocalDateTime nextRetryAt,
            LocalDateTime updatedAt
    );

    // 재시도 간격과 최대 횟수를 판단할 수 있도록 현재 처리 시도 횟수를 조회합니다.
    Optional<Integer> findAttemptCount(Long cleanupJobId);
}

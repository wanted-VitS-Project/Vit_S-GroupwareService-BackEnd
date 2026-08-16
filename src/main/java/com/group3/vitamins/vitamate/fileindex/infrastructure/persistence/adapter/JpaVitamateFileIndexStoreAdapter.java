package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort.FileIndexStatusUpdateResult;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort.ReclaimResult;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.entity.FileIndexEntity;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository.FileIndexJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// 비타메이트 파일 인덱싱 상태를 file_index 테이블에 저장하는 adapter입니다.
@Component
@RequiredArgsConstructor
public class JpaVitamateFileIndexStoreAdapter implements VitamateFileIndexStorePort {

    private final FileIndexJpaRepository fileIndexJpaRepository;

    @Override
    public boolean existsFileVersion(Long fileVersionId) {
        // DB 방언별 boolean 변환 차이를 피하기 위해 count로 존재 여부를 확인합니다.
        return fileIndexJpaRepository.countActiveFileVersion(fileVersionId) > 0;
    }

    @Override
    public List<Long> findReclaimableFileVersionIdCandidates(LocalDateTime now, int limit) {
        return fileIndexJpaRepository.findReclaimableFileVersionIdCandidates(now, FileIndexLeasePolicy.MAX_RETRY_COUNT, limit);
    }

    @Override
    public List<Long> findExhaustedFileVersionIdCandidates(LocalDateTime now, int limit) {
        return fileIndexJpaRepository.findExhaustedFileVersionIdCandidates(now, FileIndexLeasePolicy.MAX_RETRY_COUNT, limit);
    }

    @Override
    @Transactional
    public ReclaimResult claimForRetry(Long fileVersionId, LocalDateTime now) {
        String newAttemptId = UUID.randomUUID().toString();
        int updatedCount = fileIndexJpaRepository.claimForRetry(
                fileVersionId, now, FileIndexLeasePolicy.MAX_RETRY_COUNT, newAttemptId, now.plus(FileIndexLeasePolicy.LEASE_DURATION)
        );
        return updatedCount == 1 ? new ReclaimResult(true, newAttemptId) : ReclaimResult.notClaimed();
    }

    @Override
    @Transactional
    public boolean failExhausted(Long fileVersionId, LocalDateTime now, String errorMessage) {
        int updatedCount = fileIndexJpaRepository.failExhausted(fileVersionId, now, FileIndexLeasePolicy.MAX_RETRY_COUNT, errorMessage);
        return updatedCount == 1;
    }

    @Override
    @Transactional
    public FileIndexStatusUpdateResult upsertStatus(
            Long fileVersionId,
            String indexAttemptId,
            FileIndexStatus indexStatus,
            String errorMessage,
            boolean retryable,
            LocalDateTime now
    ) {
        LocalDateTime indexedAt = indexStatus == FileIndexStatus.COMPLETED ? now : null;
        String normalizedErrorMessage = indexStatus == FileIndexStatus.FAILED ? errorMessage : null;
        String resolvedAttemptId = resolveAttemptId(indexAttemptId, indexStatus);

        if (indexStatus == FileIndexStatus.COMPLETED || indexStatus == FileIndexStatus.FAILED) {
            if (resolvedAttemptId == null) {
                return new FileIndexStatusUpdateResult(false, null, indexStatus, "INDEX_ATTEMPT_REQUIRED", false);
            }

            int updatedCount = fileIndexJpaRepository.updateStatusWhenAttemptMatches(
                    fileVersionId, resolvedAttemptId, indexStatus.name(), normalizedErrorMessage, indexedAt, now
            );

            if (updatedCount == 0) {
                return new FileIndexStatusUpdateResult(false, resolvedAttemptId, indexStatus, "INDEX_ATTEMPT_MISMATCH", false);
            }

            if (indexStatus == FileIndexStatus.FAILED && retryable) {
                FileIndexStatusUpdateResult retried = tryImmediateRetry(fileVersionId, now);
                if (retried != null) {
                    return retried;
                }
            }

            return new FileIndexStatusUpdateResult(true, resolvedAttemptId, indexStatus, null, false);
        }

        fileIndexJpaRepository.upsertStatus(
                fileVersionId,
                resolvedAttemptId,
                indexStatus.name(),
                normalizedErrorMessage,
                now,
                now.plus(FileIndexLeasePolicy.LEASE_DURATION),
                indexedAt,
                now
        );

        FileIndexStatus savedStatus = fileIndexJpaRepository.findById(fileVersionId)
                .orElseThrow(() -> new IllegalStateException("file_index upsert failed"))
                .getIndexStatus();

        return new FileIndexStatusUpdateResult(true, resolvedAttemptId, savedStatus, null, false);
    }

    // FAILED로 확정한 직후, 재시도 상한 미만이면 같은 트랜잭션에서 바로 PENDING + 새 attemptId로
    // 되돌린다. 상한을 이미 채웠으면 null을 반환해 방금 확정한 FAILED를 그대로 둔다.
    private FileIndexStatusUpdateResult tryImmediateRetry(Long fileVersionId, LocalDateTime now) {
        FileIndexEntity entity = fileIndexJpaRepository.findById(fileVersionId).orElse(null);
        if (entity == null || entity.getRetryCount() >= FileIndexLeasePolicy.MAX_RETRY_COUNT) {
            return null;
        }

        String newAttemptId = UUID.randomUUID().toString();
        int updatedCount = fileIndexJpaRepository.retryAfterFailure(
                fileVersionId, newAttemptId, now.plus(FileIndexLeasePolicy.LEASE_DURATION), now
        );
        if (updatedCount == 0) {
            return null;
        }

        return new FileIndexStatusUpdateResult(true, newAttemptId, FileIndexStatus.PENDING, null, true);
    }

    // PROCESSING/PENDING은 새 시도를 열 수 있고, 완료/실패는 worker가 받은 시도 ID가 반드시 필요합니다.
    private String resolveAttemptId(String indexAttemptId, FileIndexStatus indexStatus) {
        if (indexAttemptId != null && !indexAttemptId.isBlank()) {
            return indexAttemptId;
        }

        if (indexStatus == FileIndexStatus.PENDING || indexStatus == FileIndexStatus.PROCESSING) {
            return UUID.randomUUID().toString();
        }

        return null;
    }
}

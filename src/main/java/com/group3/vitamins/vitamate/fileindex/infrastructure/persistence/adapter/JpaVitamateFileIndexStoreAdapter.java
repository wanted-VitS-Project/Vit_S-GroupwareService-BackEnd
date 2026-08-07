package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort.FileIndexStatusUpdateResult;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository.FileIndexJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public List<Long> findStalePendingFileVersionIds(LocalDateTime before) {
        return fileIndexJpaRepository.findStalePendingFileVersionIds(before);
    }

    @Override
    @Transactional
    public FileIndexStatusUpdateResult upsertStatus(
            Long fileVersionId,
            String indexAttemptId,
            FileIndexStatus indexStatus,
            String errorMessage,
            LocalDateTime now
    ) {
        LocalDateTime indexedAt = indexStatus == FileIndexStatus.COMPLETED ? now : null;
        String normalizedErrorMessage = indexStatus == FileIndexStatus.FAILED ? errorMessage : null;
        String resolvedAttemptId = resolveAttemptId(indexAttemptId, indexStatus);

        if (indexStatus == FileIndexStatus.COMPLETED || indexStatus == FileIndexStatus.FAILED) {
            if (resolvedAttemptId == null) {
                return new FileIndexStatusUpdateResult(
                        false,
                        null,
                        indexStatus,
                        "INDEX_ATTEMPT_REQUIRED"
                );
            }

            int updatedCount = fileIndexJpaRepository.updateStatusWhenAttemptMatches(
                    fileVersionId,
                    resolvedAttemptId,
                    indexStatus.name(),
                    normalizedErrorMessage,
                    indexedAt,
                    now
            );

            if (updatedCount == 0) {
                return new FileIndexStatusUpdateResult(
                        false,
                        resolvedAttemptId,
                        indexStatus,
                        "INDEX_ATTEMPT_MISMATCH"
                );
            }

            return new FileIndexStatusUpdateResult(true, resolvedAttemptId, indexStatus, null);
        }

        fileIndexJpaRepository.upsertStatus(
                fileVersionId,
                resolvedAttemptId,
                indexStatus.name(),
                normalizedErrorMessage,
                indexedAt,
                now
        );

        FileIndexStatus savedStatus = fileIndexJpaRepository.findById(fileVersionId)
                .orElseThrow(() -> new IllegalStateException("file_index upsert failed"))
                .getIndexStatus();

        return new FileIndexStatusUpdateResult(true, resolvedAttemptId, savedStatus, null);
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

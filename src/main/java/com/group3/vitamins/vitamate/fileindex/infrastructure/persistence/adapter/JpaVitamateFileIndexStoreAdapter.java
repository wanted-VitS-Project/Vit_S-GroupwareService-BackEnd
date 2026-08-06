package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository.FileIndexJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Adapter that persists Vitamate file indexing status in file_index.
@Component
@RequiredArgsConstructor
public class JpaVitamateFileIndexStoreAdapter implements VitamateFileIndexStorePort {

    private final FileIndexJpaRepository fileIndexJpaRepository;

    @Override
    public boolean existsFileVersion(Long fileVersionId) {
        // Uses count because native boolean conversion differs by database dialect.
        return fileIndexJpaRepository.countActiveFileVersion(fileVersionId) > 0;
    }

    @Override
    @Transactional
    public FileIndexStatus upsertStatus(
            Long fileVersionId,
            FileIndexStatus indexStatus,
            String errorMessage,
            LocalDateTime now
    ) {
        LocalDateTime indexedAt = indexStatus == FileIndexStatus.COMPLETED ? now : null;
        String normalizedErrorMessage = indexStatus == FileIndexStatus.FAILED ? errorMessage : null;

        fileIndexJpaRepository.upsertStatus(
                fileVersionId,
                indexStatus.name(),
                normalizedErrorMessage,
                indexedAt,
                now
        );

        return fileIndexJpaRepository.findById(fileVersionId)
                .orElseThrow(() -> new IllegalStateException("file_index upsert failed"))
                .getIndexStatus();
    }
}

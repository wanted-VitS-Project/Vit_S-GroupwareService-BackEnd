package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter;

import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.infrastructure.persistence.SpringDataFileRepository;
import com.group3.vitamins.file.infrastructure.persistence.SpringDataFileVersionRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.DocumentChunkJpaRepository;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.ChunkEmbedding;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.SavedDocumentChunk;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.SavedDocumentChunks;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.mapper.DocumentChunkBatchMapper;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository.FileIndexJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 파일 인덱싱 소스 조회와 document_chunk 저장을 JPA로 처리합니다.
@Repository
@RequiredArgsConstructor
public class JpaVitamateFileIndexDataAdapter implements VitamateFileIndexDataPort {

    private static final String COMPLETED_UPLOAD_STATUS = "COMPLETED";

    private final SpringDataFileVersionRepository fileVersionRepository;
    private final SpringDataFileRepository fileRepository;
    private final DocumentChunkJpaRepository documentChunkRepository;
    private final DocumentChunkBatchMapper documentChunkBatchMapper;
    private final FileIndexJpaRepository fileIndexRepository;
    private final FileStoragePort fileStoragePort;
    private final EntityManager entityManager;

    @Override
    public Optional<VitamateFileIndexSourceResult> findIndexSource(Long fileVersionId) {
        return fileVersionRepository.findById(fileVersionId)
                .filter(version -> COMPLETED_UPLOAD_STATUS.equals(version.getUploadStatus()))
                .filter(version -> version.getDeletedAt() == null)
                .flatMap(version -> fileRepository.findById(version.getFileId())
                        .filter(file -> file.getDeletedAt() == null)
                        .map(file -> {
                            FileStoragePort.PresignedUrl presigned = fileStoragePort.presignDownload(
                                    version.getStorageKey(),
                                    version.getOriginalFileName()
                            );

                            return new VitamateFileIndexSourceResult(
                                    version.getFileVersionId(),
                                    version.getFileId(),
                                    file.getProjectId(),
                                    version.getOriginalFileName(),
                                    version.getExtension(),
                                    version.getMimeType(),
                                    version.getSizeBytes(),
                                    version.getStorageKey(),
                                    presigned.url()
                            );
                        }));
    }

    @Override
    public boolean existsIndexableFileVersionForUpdate(Long fileVersionId) {
        return !entityManager.createNativeQuery("""
                        SELECT fv.file_version_id
                          FROM file_version fv
                               JOIN `file` f ON f.file_id = fv.file_id
                         WHERE fv.file_version_id = :fileVersionId
                           AND fv.upload_status = :uploadStatus
                           AND fv.deleted_at IS NULL
                           AND f.deleted_at IS NULL
                         FOR UPDATE
                        """)
                .setParameter("fileVersionId", fileVersionId)
                .setParameter("uploadStatus", COMPLETED_UPLOAD_STATUS)
                .getResultList()
                .isEmpty();
    }

    @Override
    public SavedDocumentChunks replaceChunks(
            Long fileVersionId,
            List<SaveVitamateDocumentChunksCommand.ChunkCommand> chunks
    ) {
        LocalDateTime now = LocalDateTime.now();
        String indexAttemptId = UUID.randomUUID().toString();

        fileIndexRepository.upsertStatus(
                fileVersionId,
                indexAttemptId,
                "PENDING",
                null,
                now,
                now.plus(FileIndexLeasePolicy.LEASE_DURATION),
                null,
                now
        );

        List<Integer> chunkIndexes = chunks.stream()
                .map(SaveVitamateDocumentChunksCommand.ChunkCommand::chunkIndex)
                .toList();

        documentChunkRepository.softDeleteMissingChunks(fileVersionId, chunkIndexes, now);

        documentChunkBatchMapper.upsertChunks(fileVersionId, chunks, now);

        List<SavedDocumentChunk> savedChunks = documentChunkRepository.findActiveByFileVersionIdAndChunkIndexIn(fileVersionId, chunkIndexes)
                .stream()
                .map(chunk -> new SavedDocumentChunk(
                        chunk.getId(),
                        chunk.getChunkIndex(),
                        chunk.getEmbeddingStatus()
                ))
                .toList();

        return new SavedDocumentChunks(indexAttemptId, savedChunks);
    }

    @Override
    public int updateChunkEmbeddings(
            Long fileVersionId,
            String indexAttemptId,
            String embeddingModel,
            List<ChunkEmbedding> chunks
    ) {
        LocalDateTime now = LocalDateTime.now();

        if (fileIndexRepository.findCurrentAttemptForUpdate(fileVersionId, indexAttemptId).isEmpty()) {
            return 0;
        }

        List<Long> documentChunkIds = chunks.stream()
                .map(ChunkEmbedding::documentChunkId)
                .toList();

        long activeChunkCount = documentChunkRepository.countActiveByFileVersionIdAndIdIn(
                fileVersionId,
                documentChunkIds
        );

        if (activeChunkCount != documentChunkIds.size()) {
            return 0;
        }

        return documentChunkBatchMapper.updateChunkEmbeddings(
                fileVersionId, chunks, embeddingModel, now
        );
    }
}

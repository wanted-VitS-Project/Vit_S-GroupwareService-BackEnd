package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence;

import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateFileDerivedDataCleanupPort;
import com.group3.vitamins.vitamate.filecleanup.application.result.CleanupVitamateFileDerivedDataResult;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Deletes Vitamate derived data that belongs to all versions of one file.
@Component
@RequiredArgsConstructor
public class JpaVitamateFileDerivedDataCleanupAdapter implements VitamateFileDerivedDataCleanupPort {

    private final EntityManager entityManager;

    @Override
    public CleanupVitamateFileDerivedDataResult cleanupByFileId(Long fileId) {
        int deletedCitationCount = deleteCitations(fileId);
        int deletedAnalysisDocumentCount = deleteAnalysisDocuments(fileId);
        int deletedDocumentChunkCount = deleteDocumentChunks(fileId);
        int deletedFileIndexCount = deleteFileIndexes(fileId);

        return new CleanupVitamateFileDerivedDataResult(
                fileId,
                deletedCitationCount,
                deletedAnalysisDocumentCount,
                deletedDocumentChunkCount,
                deletedFileIndexCount
        );
    }

    // Deletes citations first because they reference chunks and analysis-document mappings.
    private int deleteCitations(Long fileId) {
        return entityManager.createNativeQuery("""
                DELETE FROM vitamate_analysis_citation
                WHERE document_chunk_id IN (
                    SELECT dc.document_chunk_id
                    FROM document_chunk dc
                    JOIN file_version fv
                        ON fv.file_version_id = dc.file_version_id
                    WHERE fv.file_id = :fileId
                )
                OR vitamate_analysis_document_id IN (
                    SELECT vad.vitamate_analysis_document_id
                    FROM vitamate_analysis_document vad
                    JOIN file_version fv
                        ON fv.file_version_id = vad.file_version_id
                    WHERE fv.file_id = :fileId
                )
                """)
                .setParameter("fileId", fileId)
                .executeUpdate();
    }

    // Deletes mappings between analysis requests and the removed file versions.
    private int deleteAnalysisDocuments(Long fileId) {
        return entityManager.createNativeQuery("""
                DELETE FROM vitamate_analysis_document
                WHERE file_version_id IN (
                    SELECT file_version_id
                    FROM file_version
                    WHERE file_id = :fileId
                )
                """)
                .setParameter("fileId", fileId)
                .executeUpdate();
    }

    // Deletes document chunks created from the removed file versions.
    private int deleteDocumentChunks(Long fileId) {
        return entityManager.createNativeQuery("""
                DELETE FROM document_chunk
                WHERE file_version_id IN (
                    SELECT file_version_id
                    FROM file_version
                    WHERE file_id = :fileId
                )
                """)
                .setParameter("fileId", fileId)
                .executeUpdate();
    }

    // Deletes indexing status rows after dependent chunk data has been removed.
    private int deleteFileIndexes(Long fileId) {
        return entityManager.createNativeQuery("""
                DELETE FROM file_index
                WHERE file_version_id IN (
                    SELECT file_version_id
                    FROM file_version
                    WHERE file_id = :fileId
                )
                """)
                .setParameter("fileId", fileId)
                .executeUpdate();
    }
}

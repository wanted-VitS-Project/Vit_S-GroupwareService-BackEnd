package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence;

import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.infrastructure.persistence.SpringDataFileRepository;
import com.group3.vitamins.file.infrastructure.persistence.SpringDataFileVersionRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.DocumentChunkJpaRepository;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand.ChunkCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.ChunkEmbedding;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.SavedDocumentChunks;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter.JpaVitamateFileIndexDataAdapter;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository.FileIndexJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vitamate-file-index-data-adapter;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaVitamateFileIndexDataAdapter")
class JpaVitamateFileIndexDataAdapterTest {

    private static final Long PROJECT_ID = 900001L;
    private static final Long FILE_ID = 900001L;
    private static final Long FILE_VERSION_ID = 900001L;
    private static final String INDEX_ATTEMPT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 18, 30);

    @Autowired
    private SpringDataFileVersionRepository fileVersionRepository;

    @Autowired
    private SpringDataFileRepository fileRepository;

    @Autowired
    private DocumentChunkJpaRepository documentChunkRepository;

    @Autowired
    private FileIndexJpaRepository fileIndexRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private FileStoragePort fileStoragePort;
    private JpaVitamateFileIndexDataAdapter adapter;

    @BeforeEach
    void setUp() {
        fileStoragePort = mock(FileStoragePort.class);
        adapter = new JpaVitamateFileIndexDataAdapter(
                fileVersionRepository,
                fileRepository,
                documentChunkRepository,
                fileIndexRepository,
                fileStoragePort,
                entityManager
        );

        jdbcTemplate.update("DELETE FROM document_chunk");
        jdbcTemplate.update("DELETE FROM file_index");
        jdbcTemplate.update("DELETE FROM file_version");
        jdbcTemplate.update("DELETE FROM `file`");
        insertFile(FILE_ID, PROJECT_ID, null);
        insertFileVersion(FILE_VERSION_ID, FILE_ID, "COMPLETED", null);
    }

    @Nested
    @DisplayName("index source lookup")
    class FindIndexSource {

        @Test
        @DisplayName("returns file metadata and presigned download url")
        void returnsFileMetadataAndDownloadUrl() {
            when(fileStoragePort.presignDownload("local/vitamate-test/rfp.pdf", "proposal.pdf"))
                    .thenReturn(new FileStoragePort.PresignedUrl(
                            "https://example.com/download",
                            Instant.parse("2026-08-06T09:30:00Z")
                    ));

            Optional<VitamateFileIndexSourceResult> result = adapter.findIndexSource(FILE_VERSION_ID);

            assertThat(result).isPresent();
            assertThat(result.orElseThrow().fileVersionId()).isEqualTo(FILE_VERSION_ID);
            assertThat(result.orElseThrow().fileId()).isEqualTo(FILE_ID);
            assertThat(result.orElseThrow().projectId()).isEqualTo(PROJECT_ID);
            assertThat(result.orElseThrow().downloadUrl()).isEqualTo("https://example.com/download");
        }

        @Test
        @DisplayName("returns empty when file version is not completed")
        void returnsEmptyWhenFileVersionIsNotCompleted() {
            insertFileVersion(900002L, FILE_ID, "UPLOADING", null);

            Optional<VitamateFileIndexSourceResult> result = adapter.findIndexSource(900002L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when file or version is deleted")
        void returnsEmptyWhenFileOrVersionIsDeleted() {
            insertFile(900002L, PROJECT_ID, NOW);
            insertFileVersion(900003L, 900002L, "COMPLETED", null);
            insertFileVersion(900004L, FILE_ID, "COMPLETED", NOW);

            assertThat(adapter.findIndexSource(900003L)).isEmpty();
            assertThat(adapter.findIndexSource(900004L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("document chunk replace")
    class ReplaceChunks {

        @Test
        @DisplayName("checks indexable file version with DB lock without presigned url")
        void checksIndexableFileVersionWithoutPresignedUrl() {
            boolean exists = adapter.existsIndexableFileVersionForUpdate(FILE_VERSION_ID);

            assertThat(exists).isTrue();
            verifyNoInteractions(fileStoragePort);
        }

        @Test
        @DisplayName("returns false when file version is not indexable")
        void returnsFalseWhenFileVersionIsNotIndexable() {
            insertFileVersion(900002L, FILE_ID, "UPLOADING", null);

            boolean exists = adapter.existsIndexableFileVersionForUpdate(900002L);

            assertThat(exists).isFalse();
            verifyNoInteractions(fileStoragePort);
        }

        @Test
        @DisplayName("upserts existing chunk and returns document_chunk_id")
        void upsertsExistingChunkWithoutChangingId() {
            Long existingChunkId = insertDocumentChunk(FILE_VERSION_ID, 0, "기존 청크", null);

            SavedDocumentChunks savedChunks = adapter.replaceChunks(
                    FILE_VERSION_ID,
                    List.of(chunk(0, "수정된 청크"))
            );

            Long currentChunkId = findChunkId(FILE_VERSION_ID, 0);
            assertThat(savedChunks.indexAttemptId()).isNotBlank();
            assertThat(savedChunks.chunks()).hasSize(1);
            assertThat(savedChunks.chunks().get(0).documentChunkId()).isEqualTo(existingChunkId);
            assertThat(savedChunks.chunks().get(0).chunkIndex()).isEqualTo(0);
            assertThat(savedChunks.chunks().get(0).embeddingStatus()).isEqualTo("PENDING");
            assertThat(currentChunkId).isEqualTo(existingChunkId);
            assertThat(findChunkExcerpt(FILE_VERSION_ID, 0)).isEqualTo("수정된 청크");
            assertThat(findChunkDeletedAt(FILE_VERSION_ID, 0)).isNull();
        }

        @Test
        @DisplayName("resets completed embedding metadata when chunk is saved again")
        void resetsCompletedEmbeddingMetadataWhenChunkIsSavedAgain() {
            Long existingChunkId = insertDocumentChunk(FILE_VERSION_ID, 0, "completed chunk", null);
            markChunkCompleted(existingChunkId, "chroma-old", "gemini-embedding-001");

            SavedDocumentChunks savedChunks = adapter.replaceChunks(
                    FILE_VERSION_ID,
                    List.of(chunk(0, "updated chunk"))
            );

            assertThat(savedChunks.chunks()).hasSize(1);
            assertThat(savedChunks.chunks().get(0).documentChunkId()).isEqualTo(existingChunkId);
            assertThat(findChunkChromaId(existingChunkId)).isNull();
            assertThat(findChunkEmbeddingModel(existingChunkId)).isNull();
            assertThat(findChunkEmbeddingStatus(existingChunkId)).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("soft deletes chunks missing from replacement request")
        void softDeletesMissingChunks() {
            insertDocumentChunk(FILE_VERSION_ID, 0, "남길 청크", null);
            insertDocumentChunk(FILE_VERSION_ID, 1, "빠진 청크", null);

            SavedDocumentChunks savedChunks = adapter.replaceChunks(
                    FILE_VERSION_ID,
                    List.of(chunk(0, "남길 청크 수정"))
            );

            assertThat(savedChunks.chunks()).hasSize(1);
            assertThat(findChunkDeletedAt(FILE_VERSION_ID, 0)).isNull();
            assertThat(findChunkDeletedAt(FILE_VERSION_ID, 1)).isNotNull();
        }
    }

    @Nested
    @DisplayName("document chunk embedding update")
    class UpdateChunkEmbeddings {

        @Test
        @DisplayName("updates chroma id and embedding status for active chunks")
        void updatesChunkEmbeddings() {
            Long chunkId = insertDocumentChunk(FILE_VERSION_ID, 0, "인덱싱 대상 청크", null);

            insertFileIndex(FILE_VERSION_ID, INDEX_ATTEMPT_ID);

            int updatedCount = adapter.updateChunkEmbeddings(
                    FILE_VERSION_ID,
                    INDEX_ATTEMPT_ID,
                    "gemini-embedding-001",
                    List.of(new ChunkEmbedding(chunkId, "vitamate:document-chunk:" + chunkId))
            );

            assertThat(updatedCount).isEqualTo(1);
            assertThat(findChunkChromaId(chunkId)).isEqualTo("vitamate:document-chunk:" + chunkId);
            assertThat(findChunkEmbeddingModel(chunkId)).isEqualTo("gemini-embedding-001");
            assertThat(findChunkEmbeddingStatus(chunkId)).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("returns zero when chunk does not belong to requested file version")
        void returnsZeroWhenChunkDoesNotBelongToFileVersion() {
            insertFileVersion(900002L, FILE_ID, "COMPLETED", null);
            Long otherChunkId = insertDocumentChunk(900002L, 0, "다른 파일 버전 청크", null);

            insertFileIndex(FILE_VERSION_ID, INDEX_ATTEMPT_ID);

            int updatedCount = adapter.updateChunkEmbeddings(
                    FILE_VERSION_ID,
                    INDEX_ATTEMPT_ID,
                    "gemini-embedding-001",
                    List.of(new ChunkEmbedding(otherChunkId, "vitamate:document-chunk:" + otherChunkId))
            );

            assertThat(updatedCount).isZero();
            assertThat(findChunkEmbeddingStatus(otherChunkId)).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("returns zero when index attempt does not match")
        void returnsZeroWhenIndexAttemptDoesNotMatch() {
            Long chunkId = insertDocumentChunk(FILE_VERSION_ID, 0, "embedding chunk", null);
            insertFileIndex(FILE_VERSION_ID, INDEX_ATTEMPT_ID);

            int updatedCount = adapter.updateChunkEmbeddings(
                    FILE_VERSION_ID,
                    "11111111-1111-1111-1111-111111111111",
                    "gemini-embedding-001",
                    List.of(new ChunkEmbedding(chunkId, "vitamate:document-chunk:" + chunkId))
            );

            assertThat(updatedCount).isZero();
            assertThat(findChunkChromaId(chunkId)).isNull();
            assertThat(findChunkEmbeddingStatus(chunkId)).isEqualTo("PENDING");
        }
    }

    // file 테이블의 부모 데이터를 준비합니다.
    private void insertFile(Long fileId, Long projectId, LocalDateTime deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO `file` (
                    file_id, project_id, name, created_by, deleted_at
                )
                VALUES (?, ?, '제안요청서', 'EMP001', ?)
                """,
                fileId,
                projectId,
                deletedAt
        );
    }

    // file_version 테이블의 부모 데이터를 준비합니다.
    private void insertFileVersion(Long fileVersionId, Long fileId, String uploadStatus, LocalDateTime deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO file_version (
                    file_version_id, file_id, version_no, upload_status,
                    storage_key, original_file_name, extension, mime_type,
                    size_bytes, checksum, page_count, comment,
                    uploaded_by, uploader_name, uploader_department, uploader_position,
                    completed_at, deleted_at
                )
                VALUES (?, ?, ?, ?,
                        'local/vitamate-test/rfp.pdf', 'proposal.pdf', 'pdf', 'application/pdf',
                        1024, 'checksum', 1, null,
                        'EMP001', 'Tester', 'Dev', 'Staff',
                        ?, ?)
                """,
                fileVersionId,
                fileId,
                fileVersionId.intValue(),
                uploadStatus,
                NOW,
                deletedAt
        );
    }

    // 기존 document_chunk 행을 직접 만듭니다.
    private Long insertDocumentChunk(Long fileVersionId, int chunkIndex, String excerpt, LocalDateTime deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO document_chunk (
                    file_version_id, chunk_index, page_number, section_title,
                    start_offset, end_offset, token_count, excerpt,
                    embedding_status, created_at, updated_at, deleted_at
                )
                VALUES (?, ?, 1, '테스트 섹션',
                        0, 80, 30, ?,
                        'PENDING', ?, ?, ?)
                """,
                fileVersionId,
                chunkIndex,
                excerpt,
                NOW,
                NOW,
                deletedAt
        );

        return findChunkId(fileVersionId, chunkIndex);
    }

    // 저장 요청용 chunk command를 만듭니다.
    // file_index의 현재 인덱싱 시도 row를 준비합니다.
    private void insertFileIndex(Long fileVersionId, String indexAttemptId) {
        jdbcTemplate.update("""
                INSERT INTO file_index (
                    file_version_id, index_attempt_id, index_status,
                    created_at, updated_at, deleted_at
                )
                VALUES (?, ?, 'PENDING', ?, ?, NULL)
                """,
                fileVersionId,
                indexAttemptId,
                NOW,
                NOW
        );
    }

    // 기존 chunk가 이미 ChromaDB와 연결된 상태를 준비합니다.
    private void markChunkCompleted(Long documentChunkId, String chromaId, String embeddingModel) {
        jdbcTemplate.update("""
                UPDATE document_chunk
                   SET chroma_id = ?,
                       embedding_model = ?,
                       embedding_status = 'COMPLETED',
                       updated_at = ?
                 WHERE document_chunk_id = ?
                """,
                chromaId,
                embeddingModel,
                NOW,
                documentChunkId
        );
    }

    private ChunkCommand chunk(int chunkIndex, String excerpt) {
        return new ChunkCommand(
                chunkIndex,
                1,
                "테스트 섹션",
                0,
                80,
                30,
                excerpt
        );
    }

    private Long findChunkId(Long fileVersionId, int chunkIndex) {
        return jdbcTemplate.queryForObject(
                "SELECT document_chunk_id FROM document_chunk WHERE file_version_id = ? AND chunk_index = ?",
                Long.class,
                fileVersionId,
                chunkIndex
        );
    }

    private String findChunkExcerpt(Long fileVersionId, int chunkIndex) {
        return jdbcTemplate.queryForObject(
                "SELECT excerpt FROM document_chunk WHERE file_version_id = ? AND chunk_index = ?",
                String.class,
                fileVersionId,
                chunkIndex
        );
    }

    private LocalDateTime findChunkDeletedAt(Long fileVersionId, int chunkIndex) {
        return jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM document_chunk WHERE file_version_id = ? AND chunk_index = ?",
                LocalDateTime.class,
                fileVersionId,
                chunkIndex
        );
    }

    private String findChunkChromaId(Long documentChunkId) {
        return jdbcTemplate.queryForObject(
                "SELECT chroma_id FROM document_chunk WHERE document_chunk_id = ?",
                String.class,
                documentChunkId
        );
    }

    private String findChunkEmbeddingModel(Long documentChunkId) {
        return jdbcTemplate.queryForObject(
                "SELECT embedding_model FROM document_chunk WHERE document_chunk_id = ?",
                String.class,
                documentChunkId
        );
    }

    private String findChunkEmbeddingStatus(Long documentChunkId) {
        return jdbcTemplate.queryForObject(
                "SELECT embedding_status FROM document_chunk WHERE document_chunk_id = ?",
                String.class,
                documentChunkId
        );
    }
}

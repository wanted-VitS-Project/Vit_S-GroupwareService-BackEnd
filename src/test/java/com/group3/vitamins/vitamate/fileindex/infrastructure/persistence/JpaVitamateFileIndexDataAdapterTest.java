package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence;

import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.infrastructure.persistence.SpringDataFileRepository;
import com.group3.vitamins.file.infrastructure.persistence.SpringDataFileVersionRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.DocumentChunkJpaRepository;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand.ChunkCommand;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter.JpaVitamateFileIndexDataAdapter;
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
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 18, 30);

    @Autowired
    private SpringDataFileVersionRepository fileVersionRepository;

    @Autowired
    private SpringDataFileRepository fileRepository;

    @Autowired
    private DocumentChunkJpaRepository documentChunkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FileStoragePort fileStoragePort;
    private JpaVitamateFileIndexDataAdapter adapter;

    @BeforeEach
    void setUp() {
        fileStoragePort = mock(FileStoragePort.class);
        adapter = new JpaVitamateFileIndexDataAdapter(
                fileVersionRepository,
                fileRepository,
                documentChunkRepository,
                fileStoragePort
        );

        jdbcTemplate.update("DELETE FROM document_chunk");
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
                    .thenReturn(new FileStoragePort.PresignedUrl("https://example.com/download", Instant.parse("2026-08-06T09:30:00Z")));

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
        @DisplayName("upserts existing chunk without changing document_chunk_id")
        void upsertsExistingChunkWithoutChangingId() {
            Long existingChunkId = insertDocumentChunk(FILE_VERSION_ID, 0, "기존 청크", null);

            int savedCount = adapter.replaceChunks(FILE_VERSION_ID, List.of(chunk(0, "수정된 청크")));

            Long currentChunkId = findChunkId(FILE_VERSION_ID, 0);
            assertThat(savedCount).isEqualTo(1);
            assertThat(currentChunkId).isEqualTo(existingChunkId);
            assertThat(findChunkExcerpt(FILE_VERSION_ID, 0)).isEqualTo("수정된 청크");
            assertThat(findChunkDeletedAt(FILE_VERSION_ID, 0)).isNull();
        }

        @Test
        @DisplayName("soft deletes chunks missing from replacement request")
        void softDeletesMissingChunks() {
            insertDocumentChunk(FILE_VERSION_ID, 0, "남길 청크", null);
            insertDocumentChunk(FILE_VERSION_ID, 1, "빠진 청크", null);

            int savedCount = adapter.replaceChunks(FILE_VERSION_ID, List.of(chunk(0, "남길 청크 수정")));

            assertThat(savedCount).isEqualTo(1);
            assertThat(findChunkDeletedAt(FILE_VERSION_ID, 0)).isNull();
            assertThat(findChunkDeletedAt(FILE_VERSION_ID, 1)).isNotNull();
        }
    }

    // file 테이블 부모 데이터를 준비한다.
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

    // file_version 테이블 부모 데이터를 준비한다.
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

    // 기존 document_chunk 행을 직접 만든다.
    private Long insertDocumentChunk(Long fileVersionId, int chunkIndex, String excerpt, LocalDateTime deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO document_chunk (
                    file_version_id, chunk_index, page_number, section_title,
                    start_offset, end_offset, token_count, excerpt,
                    embedding_status, created_at, updated_at, deleted_at
                )
                VALUES (?, ?, 1, '테스트 섹션',
                        0, 80, 30, ?,
                        'COMPLETED', ?, ?, ?)
                """,
                fileVersionId,
                chunkIndex,
                excerpt,
                NOW,
                NOW,
                deletedAt
        );

        return jdbcTemplate.queryForObject(
                "SELECT document_chunk_id FROM document_chunk WHERE file_version_id = ? AND chunk_index = ?",
                Long.class,
                fileVersionId,
                chunkIndex
        );
    }

    // 저장 요청용 청크 command를 만든다.
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
}

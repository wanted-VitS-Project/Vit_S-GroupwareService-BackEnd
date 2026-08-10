package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence;

import com.group3.vitamins.vitamate.filecleanup.application.result.CleanupVitamateFileDerivedDataResult;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.adapter.JpaVitamateFileDerivedDataCleanupAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vitamate-file-cleanup-adapter;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaVitamateFileDerivedDataCleanupAdapter.class)
@DisplayName("JpaVitamateFileDerivedDataCleanupAdapter")
class JpaVitamateFileDerivedDataCleanupAdapterTest {

    private static final Long TARGET_FILE_ID = 900001L;
    private static final Long OTHER_FILE_ID = 900002L;
    private static final Long TARGET_FILE_VERSION_ID_1 = 910001L;
    private static final Long TARGET_FILE_VERSION_ID_2 = 910002L;
    private static final Long OTHER_FILE_VERSION_ID = 920001L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 14, 0);

    @Autowired
    private JpaVitamateFileDerivedDataCleanupAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        deleteAllRows();
        insertFileVersion(TARGET_FILE_VERSION_ID_1, TARGET_FILE_ID, 1);
        insertFileVersion(TARGET_FILE_VERSION_ID_2, TARGET_FILE_ID, 2);
        insertFileVersion(OTHER_FILE_VERSION_ID, OTHER_FILE_ID, 1);

        insertFileIndex(TARGET_FILE_VERSION_ID_1);
        insertFileIndex(TARGET_FILE_VERSION_ID_2);
        insertFileIndex(OTHER_FILE_VERSION_ID);

        insertDocumentChunk(930001L, TARGET_FILE_VERSION_ID_1, 0);
        insertDocumentChunk(930002L, TARGET_FILE_VERSION_ID_2, 0);
        insertDocumentChunk(930003L, OTHER_FILE_VERSION_ID, 0);

        insertAnalysis(940001L);
        insertAnalysisDocument(950001L, 940001L, TARGET_FILE_VERSION_ID_1);
        insertAnalysisDocument(950002L, 940001L, TARGET_FILE_VERSION_ID_2);
        insertAnalysisDocument(950003L, 940001L, OTHER_FILE_VERSION_ID);

        insertCitation(960001L, 940001L, 950001L, 930001L, 1);
        insertCitation(960002L, 940001L, 950002L, 930002L, 2);
        insertCitation(960003L, 940001L, 950003L, 930003L, 3);
    }

    @Test
    @DisplayName("deletes only Vitamate derived data for target fileId")
    void deletesOnlyTargetFileDerivedData() {
        CleanupVitamateFileDerivedDataResult result = adapter.cleanupByFileId(TARGET_FILE_ID);

        assertThat(result.fileId()).isEqualTo(TARGET_FILE_ID);
        assertThat(result.deletedCitationCount()).isEqualTo(2);
        assertThat(result.deletedAnalysisDocumentCount()).isEqualTo(2);
        assertThat(result.deletedDocumentChunkCount()).isEqualTo(2);
        assertThat(result.deletedFileIndexCount()).isEqualTo(2);

        assertThat(count("vitamate_analysis_citation")).isEqualTo(1);
        assertThat(count("vitamate_analysis_document")).isEqualTo(1);
        assertThat(count("document_chunk")).isEqualTo(1);
        assertThat(count("file_index")).isEqualTo(1);

        assertThat(countByFileVersion("file_index", OTHER_FILE_VERSION_ID)).isEqualTo(1);
        assertThat(countByFileVersion("document_chunk", OTHER_FILE_VERSION_ID)).isEqualTo(1);
        assertThat(countAnalysisDocumentsByFileVersion(OTHER_FILE_VERSION_ID)).isEqualTo(1);
        assertThat(countCitationsById(960003L)).isEqualTo(1);
        assertThat(count("vitamate_analysis")).isEqualTo(1);
    }

    @Test
    @DisplayName("returns zero counts when file has no derived data")
    void returnsZeroCountsForMissingDerivedData() {
        CleanupVitamateFileDerivedDataResult result = adapter.cleanupByFileId(999999L);

        assertThat(result.fileId()).isEqualTo(999999L);
        assertThat(result.deletedCitationCount()).isZero();
        assertThat(result.deletedAnalysisDocumentCount()).isZero();
        assertThat(result.deletedDocumentChunkCount()).isZero();
        assertThat(result.deletedFileIndexCount()).isZero();
    }

    // Clears child tables first so each test starts from a predictable fixture.
    private void deleteAllRows() {
        jdbcTemplate.update("DELETE FROM vitamate_analysis_citation");
        jdbcTemplate.update("DELETE FROM vitamate_analysis_document");
        jdbcTemplate.update("DELETE FROM document_chunk");
        jdbcTemplate.update("DELETE FROM file_index");
        jdbcTemplate.update("DELETE FROM vitamate_analysis");
        jdbcTemplate.update("DELETE FROM file_version");
    }

    // Creates the minimum file_version row needed by Vitamate derived tables.
    private void insertFileVersion(Long fileVersionId, Long fileId, int versionNo) {
        jdbcTemplate.update("""
                INSERT INTO file_version (
                    file_version_id, file_id, version_no, upload_status,
                    storage_key, original_file_name, extension, mime_type,
                    size_bytes, checksum, page_count, comment,
                    uploaded_by, uploader_name, uploader_department, uploader_position,
                    completed_at, deleted_at
                )
                VALUES (?, ?, ?, 'COMPLETED',
                        ?, 'proposal.pdf', 'pdf', 'application/pdf',
                        1000, 'checksum', 1, null,
                        'EMP001', 'Tester', 'Dev', 'Staff',
                        ?, null)
                """,
                fileVersionId,
                fileId,
                versionNo,
                "files/" + fileVersionId + "/proposal.pdf",
                NOW
        );
    }

    // Creates one indexing status row for cleanup target verification.
    private void insertFileIndex(Long fileVersionId) {
        jdbcTemplate.update("""
                INSERT INTO file_index (
                    file_version_id, index_status, index_error_message,
                    indexed_at, created_at, updated_at, deleted_at
                )
                VALUES (?, 'COMPLETED', null, ?, ?, ?, null)
                """,
                fileVersionId,
                NOW,
                NOW,
                NOW
        );
    }

    // Creates one document chunk row for citation and chunk cleanup verification.
    private void insertDocumentChunk(Long documentChunkId, Long fileVersionId, int chunkIndex) {
        jdbcTemplate.update("""
                INSERT INTO document_chunk (
                    document_chunk_id, file_version_id, chunk_index,
                    page_number, section_title, start_offset, end_offset,
                    token_count, chroma_id, excerpt, embedding_model,
                    embedding_status, created_at, updated_at, deleted_at
                )
                VALUES (?, ?, ?,
                        1, 'cleanup-test', 0, 100,
                        30, null, 'cleanup excerpt', null,
                        'PENDING', ?, ?, null)
                """,
                documentChunkId,
                fileVersionId,
                chunkIndex,
                NOW,
                NOW
        );
    }

    // Creates a parent analysis row that should remain after derived data cleanup.
    private void insertAnalysis(Long analysisId) {
        jdbcTemplate.update("""
                INSERT INTO vitamate_analysis (
                    vitamate_analysis_id, vitamate_block_id, requested_by,
                    idempotency_key, request_hash, prompt, analysis_status,
                    created_at, updated_at, deleted_at
                )
                VALUES (?, 1, 'EMP001',
                        ?, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                        'analysis prompt', 'COMPLETED',
                        ?, ?, null)
                """,
                analysisId,
                "cleanup-test-" + analysisId,
                NOW,
                NOW
        );
    }

    // Creates analysis-document mapping rows for selected file versions.
    private void insertAnalysisDocument(Long analysisDocumentId, Long analysisId, Long fileVersionId) {
        jdbcTemplate.update("""
                INSERT INTO vitamate_analysis_document (
                    vitamate_analysis_document_id, vitamate_analysis_id,
                    file_version_id, document_role, created_at, deleted_at
                )
                VALUES (?, ?, ?, 'TARGET', ?, null)
                """,
                analysisDocumentId,
                analysisId,
                fileVersionId,
                NOW
        );
    }

    // Creates citation rows that must be deleted before their document/chunk parents.
    private void insertCitation(
            Long citationId,
            Long analysisId,
            Long analysisDocumentId,
            Long documentChunkId,
            int rankOrder
    ) {
        jdbcTemplate.update("""
                INSERT INTO vitamate_analysis_citation (
                    vitamate_analysis_citation_id, vitamate_analysis_id,
                    vitamate_analysis_document_id, document_chunk_id,
                    rank_order, distance_score, excerpt, created_at, deleted_at
                )
                VALUES (?, ?, ?, ?, ?, 0.123456, 'excerpt', ?, null)
                """,
                citationId,
                analysisId,
                analysisDocumentId,
                documentChunkId,
                rankOrder,
                NOW
        );
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private int countByFileVersion(String tableName, Long fileVersionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE file_version_id = ?",
                Integer.class,
                fileVersionId
        );
        return count == null ? 0 : count;
    }

    private int countAnalysisDocumentsByFileVersion(Long fileVersionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vitamate_analysis_document WHERE file_version_id = ?",
                Integer.class,
                fileVersionId
        );
        return count == null ? 0 : count;
    }

    private int countCitationsById(Long citationId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vitamate_analysis_citation WHERE vitamate_analysis_citation_id = ?",
                Integer.class,
                citationId
        );
        return count == null ? 0 : count;
    }
}

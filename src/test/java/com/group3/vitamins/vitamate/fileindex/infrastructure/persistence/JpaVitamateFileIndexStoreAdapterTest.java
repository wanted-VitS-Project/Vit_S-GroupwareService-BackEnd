package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence;

import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter.JpaVitamateFileIndexStoreAdapter;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.entity.FileIndexEntity;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository.FileIndexJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vitamate-file-index-adapter;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaVitamateFileIndexStoreAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("JpaVitamateFileIndexStoreAdapter")
class JpaVitamateFileIndexStoreAdapterTest {

    private static final Long ACTIVE_FILE_VERSION_ID = 900001L;
    private static final Long DELETED_FILE_VERSION_ID = 900002L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 10, 30);

    @Autowired
    private JpaVitamateFileIndexStoreAdapter adapter;

    @Autowired
    private FileIndexJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM file_index");
        jdbcTemplate.update("DELETE FROM file_version");
        insertFileVersion(ACTIVE_FILE_VERSION_ID, null);
        insertFileVersion(DELETED_FILE_VERSION_ID, NOW);
    }

    @Nested
    @DisplayName("file version existence check")
    class ExistsFileVersion {

        @Test
        @DisplayName("returns true for active file_version")
        void returnsTrueForActiveFileVersion() {
            assertThat(adapter.existsFileVersion(ACTIVE_FILE_VERSION_ID)).isTrue();
        }

        @Test
        @DisplayName("returns false for deleted or missing file_version")
        void returnsFalseForDeletedOrMissingFileVersion() {
            assertThat(adapter.existsFileVersion(DELETED_FILE_VERSION_ID)).isFalse();
            assertThat(adapter.existsFileVersion(999999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("file_index status upsert")
    class UpsertStatus {

        @Test
        @DisplayName("inserts PENDING status when row does not exist")
        void insertsPendingStatus() {
            FileIndexStatus saved = adapter.upsertStatus(ACTIVE_FILE_VERSION_ID, FileIndexStatus.PENDING, null, NOW);

            FileIndexEntity entity = repository.findById(ACTIVE_FILE_VERSION_ID).orElseThrow();
            assertThat(saved).isEqualTo(FileIndexStatus.PENDING);
            assertThat(entity.getIndexStatus()).isEqualTo(FileIndexStatus.PENDING);
            assertThat(entity.getIndexErrorMessage()).isNull();
            assertThat(entity.getIndexedAt()).isNull();
        }

        @Test
        @DisplayName("inserts PROCESSING status when row does not exist")
        void insertsProcessingStatus() {
            FileIndexStatus saved = adapter.upsertStatus(ACTIVE_FILE_VERSION_ID, FileIndexStatus.PROCESSING, null, NOW);

            FileIndexEntity entity = repository.findById(ACTIVE_FILE_VERSION_ID).orElseThrow();
            assertThat(saved).isEqualTo(FileIndexStatus.PROCESSING);
            assertThat(entity.getIndexStatus()).isEqualTo(FileIndexStatus.PROCESSING);
            assertThat(entity.getIndexErrorMessage()).isNull();
            assertThat(entity.getIndexedAt()).isNull();
        }

        @Test
        @DisplayName("updates to COMPLETED and clears failure message")
        void updatesToCompletedStatus() {
            adapter.upsertStatus(ACTIVE_FILE_VERSION_ID, FileIndexStatus.FAILED, "extract failed", NOW.minusMinutes(1));

            FileIndexStatus saved = adapter.upsertStatus(ACTIVE_FILE_VERSION_ID, FileIndexStatus.COMPLETED, null, NOW);

            FileIndexEntity entity = repository.findById(ACTIVE_FILE_VERSION_ID).orElseThrow();
            assertThat(saved).isEqualTo(FileIndexStatus.COMPLETED);
            assertThat(entity.getIndexStatus()).isEqualTo(FileIndexStatus.COMPLETED);
            assertThat(entity.getIndexErrorMessage()).isNull();
            assertThat(entity.getIndexedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("updates to FAILED and clears indexed_at")
        void updatesToFailedStatus() {
            adapter.upsertStatus(ACTIVE_FILE_VERSION_ID, FileIndexStatus.COMPLETED, null, NOW.minusMinutes(1));

            FileIndexStatus saved = adapter.upsertStatus(ACTIVE_FILE_VERSION_ID, FileIndexStatus.FAILED, "extract failed", NOW);

            FileIndexEntity entity = repository.findById(ACTIVE_FILE_VERSION_ID).orElseThrow();
            assertThat(saved).isEqualTo(FileIndexStatus.FAILED);
            assertThat(entity.getIndexStatus()).isEqualTo(FileIndexStatus.FAILED);
            assertThat(entity.getIndexErrorMessage()).isEqualTo("extract failed");
            assertThat(entity.getIndexedAt()).isNull();
        }

        @Test
        @DisplayName("handles concurrent first callbacks without duplicate row failure")
        void handlesConcurrentFirstCallbacks() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Callable<FileIndexStatus> callback = () -> {
                ready.countDown();
                start.await(3, TimeUnit.SECONDS);
                return adapter.upsertStatus(ACTIVE_FILE_VERSION_ID, FileIndexStatus.PROCESSING, null, NOW);
            };

            try {
                List<Future<FileIndexStatus>> futures = List.of(
                        executor.submit(callback),
                        executor.submit(callback)
                );

                assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                assertThat(futures.get(0).get(5, TimeUnit.SECONDS)).isEqualTo(FileIndexStatus.PROCESSING);
                assertThat(futures.get(1).get(5, TimeUnit.SECONDS)).isEqualTo(FileIndexStatus.PROCESSING);

                FileIndexEntity entity = repository.findById(ACTIVE_FILE_VERSION_ID).orElseThrow();
                assertThat(repository.count()).isEqualTo(1);
                assertThat(entity.getIndexStatus()).isEqualTo(FileIndexStatus.PROCESSING);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    // Prepares parent file_version rows required by file_index.
    private void insertFileVersion(Long fileVersionId, LocalDateTime deletedAt) {
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
                        ?, ?)
                """,
                fileVersionId,
                fileVersionId,
                fileVersionId.intValue(),
                "files/" + fileVersionId + "/proposal.pdf",
                NOW,
                deletedAt
        );
    }
}

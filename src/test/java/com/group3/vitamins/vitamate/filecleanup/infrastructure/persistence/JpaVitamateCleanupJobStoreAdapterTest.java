package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupJob;
import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupOutbox;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.adapter.JpaVitamateCleanupJobStoreAdapter;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupJobEntity;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupOutboxEntity;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository.VitamateCleanupJobJpaRepository;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository.VitamateCleanupOutboxJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vitamate-cleanup-job-store;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaVitamateCleanupJobStoreAdapter")
class JpaVitamateCleanupJobStoreAdapterTest {

    private static final Long FILE_ID = 900001L;
    private static final Long FILE_VERSION_ID_1 = 910001L;
    private static final Long FILE_VERSION_ID_2 = 910002L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 15, 0);

    @Autowired
    private VitamateCleanupJobJpaRepository cleanupJobRepository;

    @Autowired
    private VitamateCleanupOutboxJpaRepository cleanupOutboxRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JpaVitamateCleanupJobStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        cleanupOutboxRepository.deleteAllInBatch();
        cleanupJobRepository.deleteAllInBatch();
        jdbcTemplate.update("DELETE FROM file_version");

        adapter = new JpaVitamateCleanupJobStoreAdapter(
                cleanupJobRepository,
                cleanupOutboxRepository,
                entityManager,
                objectMapper
        );
    }

    @Test
    @DisplayName("파일 버전 목록으로 cleanup job과 outbox를 함께 생성한다")
    void createsCleanupJobAndOutbox() throws Exception {
        insertFileVersion(FILE_VERSION_ID_2, FILE_ID, 2);
        insertFileVersion(FILE_VERSION_ID_1, FILE_ID, 1);

        adapter.createCleanupJob(FILE_ID);
        entityManager.flush();
        entityManager.clear();

        List<VitamateCleanupJobEntity> jobs = cleanupJobRepository.findAll();
        List<VitamateCleanupOutboxEntity> outboxes = cleanupOutboxRepository.findAll();

        assertThat(jobs).hasSize(1);
        assertThat(outboxes).hasSize(1);

        VitamateCleanupJobEntity job = jobs.get(0);
        assertThat(job.getCleanupKey()).isNotBlank();
        assertThat(job.getFileId()).isEqualTo(FILE_ID);
        assertThat(job.getCleanupStatus()).isEqualTo(VitamateCleanupJob.Status.WAITING);
        assertThat(job.getAttemptCount()).isZero();
        assertThat(job.getDeletedVectorCount()).isZero();
        assertThat(job.getFileVersionIds())
                .isEqualTo(objectMapper.readTree("[910001,910002]"));

        VitamateCleanupOutboxEntity outbox = outboxes.get(0);
        assertThat(outbox.getEventId()).isNotBlank();
        assertThat(outbox.getCleanupJob().getCleanupJobId()).isEqualTo(job.getCleanupJobId());
        assertThat(outbox.getEventType()).isEqualTo("CHROMA_VECTOR_DELETE_REQUESTED");
        assertThat(outbox.getPublishStatus()).isEqualTo(VitamateCleanupOutbox.PublishStatus.PENDING);
        assertThat(outbox.getPublishAttemptCount()).isZero();

        JsonNode payload = outbox.getPayload();
        assertThat(payload.get("cleanupJobId").asLong()).isEqualTo(job.getCleanupJobId());
        assertThat(payload.get("cleanupKey").asText()).isEqualTo(job.getCleanupKey());
        assertThat(payload.get("fileVersionIds"))
                .isEqualTo(objectMapper.readTree("[910001,910002]"));
    }

    @Test
    @DisplayName("파일 버전이 없으면 cleanup job과 outbox를 생성하지 않는다")
    void skipsCreationWhenFileHasNoVersions() {
        adapter.createCleanupJob(FILE_ID);

        assertThat(cleanupJobRepository.count()).isZero();
        assertThat(cleanupOutboxRepository.count()).isZero();
    }

    @Test
    @DisplayName("트랜잭션이 실패하면 cleanup job과 outbox를 모두 롤백한다")
    void rollsBackCleanupJobAndOutboxTogether() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            insertFileVersion(FILE_VERSION_ID_1, FILE_ID, 1);
            adapter.createCleanupJob(FILE_ID);
            throw new IllegalStateException("cleanup failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(cleanupJobRepository.count()).isZero();
        assertThat(cleanupOutboxRepository.count()).isZero();
    }

    // cleanup 대상 파일의 버전 스냅샷을 구성합니다.
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
                        ?, 'cleanup-target.pdf', 'pdf', 'application/pdf',
                        1000, 'checksum', 1, null,
                        'EMP001', '테스터', '개발팀', '사원',
                        ?, null)
                """,
                fileVersionId,
                fileId,
                versionNo,
                "files/" + fileVersionId + "/cleanup-target.pdf",
                NOW
        );
    }
}

package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence;

import com.group3.vitamins.vitamate.filecleanup.application.model.ClaimedVitamateCleanupOutbox;
import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupJob;
import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupOutbox;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.adapter.JpaVitamateCleanupOutboxStoreAdapter;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupOutboxEntity;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository.VitamateCleanupOutboxJpaRepository;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository.VitamateCleanupJobJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vitamate-cleanup-outbox;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaVitamateCleanupOutboxStoreAdapter.class)
@DisplayName("JpaVitamateCleanupOutboxStoreAdapter")
class JpaVitamateCleanupOutboxStoreAdapterTest {

    private static final Long JOB_ID = 1001L;
    private static final Long OUTBOX_ID = 2001L;
    private static final String EVENT_ID =
            "11111111-1111-1111-1111-111111111111";

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 9, 17, 0);

    @Autowired
    private JpaVitamateCleanupOutboxStoreAdapter adapter;

    @Autowired
    private VitamateCleanupOutboxJpaRepository outboxRepository;

    @Autowired
    private VitamateCleanupJobJpaRepository jobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM vitamate_cleanup_outbox");
        jdbcTemplate.update("DELETE FROM vitamate_cleanup_job");
        insertCleanupJob();
    }

    @Test
    @DisplayName("발행 가능한 PENDING Outbox를 점유한다")
    void claimsPublishableOutbox() {
        insertOutbox(null, null, NOW.minusMinutes(1));

        List<ClaimedVitamateCleanupOutbox> claimed =
                adapter.claimPublishable(
                        "spring-instance-1",
                        10,
                        NOW,
                        NOW.plusSeconds(30)
                );

        flushAndClear();
        VitamateCleanupOutboxEntity saved = findOutbox();

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).outboxId()).isEqualTo(OUTBOX_ID);
        assertThat(claimed.get(0).cleanupJobId()).isEqualTo(JOB_ID);
        assertThat(claimed.get(0).eventId()).isEqualTo(EVENT_ID);
        assertThat(claimed.get(0).eventType())
                .isEqualTo("CHROMA_VECTOR_DELETE_REQUESTED");

        assertThat(saved.getLockOwner()).isEqualTo("spring-instance-1");
        assertThat(saved.getLockExpiresAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(saved.getPublishAttemptCount()).isEqualTo(1);
        assertThat(saved.getPublishStatus())
                .isEqualTo(VitamateCleanupOutbox.PublishStatus.PENDING);
        assertThat(claimed.get(0).attemptId()).isNotBlank();
        assertThat(claimed.get(0).retryCount()).isZero();
        assertThat(claimed.get(0).fileVersionIds())
                .containsExactly(910001L, 910002L);
    }

    @Test
    @DisplayName("다른 서버가 아직 점유 중인 Outbox는 제외한다")
    void ignoresActiveLock() {
        insertOutbox(
                "spring-instance-1",
                NOW.plusMinutes(1),
                NOW.minusMinutes(1)
        );

        List<ClaimedVitamateCleanupOutbox> claimed =
                adapter.claimPublishable(
                        "spring-instance-2",
                        10,
                        NOW,
                        NOW.plusSeconds(30)
                );

        assertThat(claimed).isEmpty();
        assertThat(findOutbox().getLockOwner())
                .isEqualTo("spring-instance-1");
    }

    @Test
    @DisplayName("잠금이 만료된 Outbox는 새로운 서버가 다시 점유한다")
    void reclaimsExpiredLock() {
        insertOutbox(
                "stopped-instance",
                NOW.minusSeconds(1),
                NOW.minusMinutes(1)
        );

        List<ClaimedVitamateCleanupOutbox> claimed =
                adapter.claimPublishable(
                        "spring-instance-2",
                        10,
                        NOW,
                        NOW.plusSeconds(30)
                );

        flushAndClear();
        VitamateCleanupOutboxEntity saved = findOutbox();

        assertThat(claimed).hasSize(1);
        assertThat(saved.getLockOwner()).isEqualTo("spring-instance-2");
        assertThat(saved.getLockExpiresAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(saved.getPublishAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Outbox를 점유한 서버만 발행 완료 처리할 수 있다")
    void marksPublishedOnlyByLockOwner() {
        insertOutbox(null, null, NOW.minusMinutes(1));

        adapter.claimPublishable(
                "spring-instance-1",
                10,
                NOW,
                NOW.plusSeconds(30)
        );

        adapter.markPublished(
                OUTBOX_ID,
                "spring-instance-2",
                NOW
        );

        flushAndClear();
        assertThat(findOutbox().getPublishStatus())
                .isEqualTo(VitamateCleanupOutbox.PublishStatus.PENDING);

        adapter.markPublished(
                OUTBOX_ID,
                "spring-instance-1",
                NOW
        );

        flushAndClear();
        VitamateCleanupOutboxEntity saved = findOutbox();

        assertThat(saved.getPublishStatus())
                .isEqualTo(VitamateCleanupOutbox.PublishStatus.PUBLISHED);
        assertThat(saved.getPublishedAt()).isEqualTo(NOW);
        assertThat(saved.getLockOwner()).isNull();
        assertThat(saved.getLockExpiresAt()).isNull();
        assertThat(jobRepository.findById(JOB_ID).orElseThrow().getCleanupStatus())
                .isEqualTo(VitamateCleanupJob.Status.PUBLISHED);
    }

    @Test
    @DisplayName("발행 실패 시 잠금을 해제하고 재시도 시각을 저장한다")
    void schedulesRetryAfterPublishFailure() {
        insertOutbox(
                "spring-instance-1",
                NOW.plusSeconds(30),
                NOW.minusMinutes(1)
        );

        LocalDateTime nextRetryAt = NOW.plusMinutes(1);

        adapter.markPublishFailed(
                OUTBOX_ID,
                "spring-instance-1",
                "Redis connection failed",
                nextRetryAt
        );

        flushAndClear();
        VitamateCleanupOutboxEntity saved = findOutbox();

        assertThat(saved.getPublishStatus())
                .isEqualTo(VitamateCleanupOutbox.PublishStatus.PENDING);
        assertThat(saved.getAvailableAt()).isEqualTo(nextRetryAt);
        assertThat(saved.getLastErrorMessage())
                .isEqualTo("Redis connection failed");
        assertThat(saved.getLockOwner()).isNull();
        assertThat(saved.getLockExpiresAt()).isNull();
    }

    // 테스트에서 사용할 cleanup job을 생성합니다.
    private void insertCleanupJob() {
        jdbcTemplate.update("""
                INSERT INTO vitamate_cleanup_job (
                    cleanup_job_id,
                    cleanup_key,
                    file_id,
                    file_version_ids,
                    cleanup_status,
                    attempt_count,
                    deleted_vector_count,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    '22222222-2222-2222-2222-222222222222',
                    900001,
                    JSON '[910001, 910002]',
                    'WAITING',
                    0,
                    0,
                    ?,
                    ?
                )
                """,
                JOB_ID,
                NOW,
                NOW
        );
    }

    // 점유 상태를 조정할 수 있는 Outbox fixture를 생성합니다.
    private void insertOutbox(
            String lockOwner,
            LocalDateTime lockExpiresAt,
            LocalDateTime availableAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO vitamate_cleanup_outbox (
                    cleanup_outbox_id,
                    event_id,
                    cleanup_job_id,
                    event_type,
                    payload,
                    publish_status,
                    publish_attempt_count,
                    available_at,
                    lock_owner,
                    lock_expires_at,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    'CHROMA_VECTOR_DELETE_REQUESTED',
                    '{"cleanupJobId":1001,"cleanupKey":"test","fileVersionIds":[910001,910002]}',
                    'PENDING',
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                OUTBOX_ID,
                EVENT_ID,
                JOB_ID,
                lockOwner == null ? 0 : 1,
                availableAt,
                lockOwner,
                lockExpiresAt,
                NOW,
                NOW
        );
    }

    private VitamateCleanupOutboxEntity findOutbox() {
        return outboxRepository.findById(OUTBOX_ID).orElseThrow();
    }

    // DB 반영 후 영속성 컨텍스트 캐시를 비워 실제 저장값을 조회합니다.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

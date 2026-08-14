package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository.CollectionConditionParamsJsonMapper;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository.CollectionConditionPersistenceMapper;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository.CollectionConditionRepositoryAdapter;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper.CollectionRunConditionSnapshotJsonMapper;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper.CollectionRunPersistenceMapper;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository.CollectionRunRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:collection-run-outbox-cleanup;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaCollectionRunOutboxStoreAdapter.class,
        CollectionRunRepositoryAdapter.class,
        CollectionRunPersistenceMapper.class,
        CollectionRunConditionSnapshotJsonMapper.class,
        CollectionConditionRepositoryAdapter.class,
        CollectionConditionPersistenceMapper.class,
        CollectionConditionParamsJsonMapper.class,
        JpaCollectionRunOutboxStoreAdapterTest.JacksonConfig.class
})
@DisplayName("JpaCollectionRunOutboxStoreAdapter PUBLISHED 행 정리")
class JpaCollectionRunOutboxStoreAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final String USER_ID = "EMP001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 21, 4, 0);
    private static final LocalDateTime CUTOFF = NOW.minusDays(7);

    @Autowired
    private JpaCollectionRunOutboxStoreAdapter outboxAdapter;

    @Autowired
    private CollectionRunRepositoryAdapter runAdapter;

    @Autowired
    private CollectionConditionRepositoryAdapter conditionAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long runId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                INSERT INTO crawl_source (
                    crawl_source_id, source_code, source_name, source_type, enabled, created_at
                )
                VALUES (1, 'NARA', '나라장터', 'OPEN_API', true, CURRENT_TIMESTAMP)
                """);

        CollectionConditionFilter filter = new CollectionConditionFilter(
                List.of("스마트시티"), List.of("11"), List.of("6202"),
                100_000_000L, 1_000_000_000L, true, InternationalBidType.DOMESTIC
        );
        CollectionCondition condition = conditionAdapter.save(CollectionCondition.create(
                COMPANY_ID, "NARA", "수도권 스마트시티 용역",
                List.of(BidNoticeType.SERVICE), filter, true, USER_ID, LocalDateTime.now()
        ));
        CollectionRunConditionSnapshot snapshot = new CollectionRunConditionSnapshot(
                condition.getSourceCode(), condition.getConditionName(),
                condition.getNoticeTypes(), condition.getFilters(),
                LocalDateTime.of(2026, 8, 9, 0, 0), LocalDateTime.of(2026, 8, 10, 0, 0)
        );
        runId = runAdapter.save(CollectionRun.createPending(
                condition.getConditionId(), snapshot, USER_ID, LocalDateTime.now()
        )).runId();
    }

    @Test
    @DisplayName("보관 기간이 지난 PUBLISHED 행만 지우고 PENDING·FAILED·최근·경계값 PUBLISHED 행은 남긴다")
    void deletesOnlyStalePublishedRows() {
        insertOutboxRow("stale-published", "PUBLISHED", NOW.minusDays(10));
        insertOutboxRow("fresh-published", "PUBLISHED", NOW.minusDays(1));
        insertOutboxRow("boundary-published", "PUBLISHED", CUTOFF);
        insertOutboxRow("stale-failed", "FAILED", null);
        insertOutboxRow("fresh-pending", "PENDING", null);

        int deletedCount = outboxAdapter.deletePublishedBefore(CUTOFF);

        assertThat(deletedCount).isEqualTo(1);
        assertThat(countByEventId("stale-published")).isZero();
        assertThat(countByEventId("fresh-published")).isEqualTo(1);
        // 쿼리 계약은 published_at < cutoff이므로 cutoff와 같은 시각은 지우면 안 된다.
        assertThat(countByEventId("boundary-published")).isEqualTo(1);
        assertThat(countByEventId("stale-failed")).isEqualTo(1);
        assertThat(countByEventId("fresh-pending")).isEqualTo(1);
    }

    private void insertOutboxRow(String eventId, String publishStatus, LocalDateTime publishedAt) {
        jdbcTemplate.update("""
                INSERT INTO crawl_run_outbox (
                    event_id, crawl_run_id, attempt_id, event_type, payload,
                    publish_status, publish_attempt_count, available_at, published_at,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, 'BID_NOTICE_COLLECTION_REQUESTED', '{}', ?, 0, ?, ?, ?, ?)
                """,
                eventId, runId, "attempt-" + eventId, publishStatus,
                NOW.minusDays(10), publishedAt, NOW.minusDays(10), NOW.minusDays(10)
        );
    }

    private int countByEventId(String eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM crawl_run_outbox WHERE event_id = ?", Integer.class, eventId
        );
        return count == null ? 0 : count;
    }

    @TestConfiguration
    static class JacksonConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        Clock clock() {
            return Clock.systemDefaultZone();
        }
    }
}

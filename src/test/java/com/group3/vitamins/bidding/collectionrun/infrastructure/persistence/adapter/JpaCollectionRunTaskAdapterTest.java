package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTaskStatus;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository.SpringDataCollectionRunTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:collection-run-task;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaCollectionRunTaskAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("JpaCollectionRunTaskAdapter 상태 전이")
class JpaCollectionRunTaskAdapterTest {

    private static final Long RUN_ID = 1L;
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 10, 20, 0);

    private static final CollectionRequestCombination TARGET =
            new CollectionRequestCombination(
                    BidNoticeType.SERVICE,
                    "스마트시티",
                    "11",
                    "6202",
                    1
            );

    @Autowired
    private JpaCollectionRunTaskAdapter adapter;

    @Autowired
    private SpringDataCollectionRunTaskRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        adapter.createTasks(RUN_ID, List.of(TARGET));
    }

    @Test
    @DisplayName("같은 작업은 동시에 한 번만 점유할 수 있다")
    void claimsTaskOnlyOnce() {
        var first = adapter.claim(
                RUN_ID, TARGET, "attempt-1", 0,
                NOW, NOW.plusMinutes(5)
        );
        var second = adapter.claim(
                RUN_ID, TARGET, "attempt-2", 0,
                NOW.plusSeconds(1), NOW.plusMinutes(5)
        );

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(first.orElseThrow().status())
                .isEqualTo(CollectionRunTaskStatus.PROCESSING);
    }

    @Test
    @DisplayName("lease가 만료된 작업은 새로운 worker가 다시 점유한다")
    void reclaimsExpiredTask() {
        adapter.claim(
                RUN_ID, TARGET, "attempt-1", 0,
                NOW, NOW.plusMinutes(1)
        );

        var reclaimed = adapter.claim(
                RUN_ID, TARGET, "attempt-2", 1,
                NOW.plusMinutes(2), NOW.plusMinutes(7)
        );

        assertThat(reclaimed).isPresent();
        assertThat(reclaimed.orElseThrow().attemptId())
                .isEqualTo("attempt-2");
        assertThat(reclaimed.orElseThrow().retryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("현재 attemptId와 일치할 때만 완료할 수 있다")
    void completesOnlyCurrentAttempt() {
        Long taskId = adapter.claim(
                RUN_ID, TARGET, "attempt-1", 0,
                NOW, NOW.plusMinutes(5)
        ).orElseThrow().taskId();

        boolean stale = adapter.complete(
                taskId, "old-attempt",
                10, 6, 3, 1, NOW.plusMinutes(1)
        );
        boolean completed = adapter.complete(
                taskId, "attempt-1",
                10, 6, 3, 1, NOW.plusMinutes(1)
        );

        assertThat(stale).isFalse();
        assertThat(completed).isTrue();

        var summary = adapter.summarize(RUN_ID);
        assertThat(summary.completedCount()).isEqualTo(1);
        assertThat(summary.collectedCount()).isEqualTo(10);
        assertThat(summary.insertedCount()).isEqualTo(6);
        assertThat(summary.updatedCount()).isEqualTo(3);
        assertThat(summary.skippedCount()).isEqualTo(1);
        assertThat(summary.isFinished()).isTrue();
    }

    @Test
    @DisplayName("대기 중인 가장 앞선 작업을 조회한다")
    void findsNextPendingTask() {
        CollectionRequestCombination secondPage = new CollectionRequestCombination(
                BidNoticeType.SERVICE,
                "스마트시티",
                "11",
                "6202",
                2
        );
        adapter.createTasks(RUN_ID, List.of(secondPage));

        var found = adapter.findNextProcessableTask(RUN_ID, NOW);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().target().pageNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("lease가 유효한 작업은 제외하고 만료된 작업은 다시 조회한다")
    void findsOnlyExpiredProcessingTask() {
        adapter.claim(
                RUN_ID, TARGET, "attempt-1", 0,
                NOW, NOW.plusMinutes(5)
        );

        assertThat(adapter.findNextProcessableTask(RUN_ID, NOW.plusMinutes(1)))
                .isEmpty();
        assertThat(adapter.findNextProcessableTask(RUN_ID, NOW.plusMinutes(6)))
                .isPresent();
    }
}

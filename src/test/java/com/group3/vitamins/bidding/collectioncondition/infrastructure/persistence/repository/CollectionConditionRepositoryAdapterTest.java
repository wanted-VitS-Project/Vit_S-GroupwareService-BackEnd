package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.*;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:collection-condition;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
// 운영 초기화 SQL 대신 Hibernate가 만든 H2 스키마만 사용하는 저장소 단위 테스트입니다.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        CollectionConditionRepositoryAdapter.class,
        CollectionConditionPersistenceMapper.class,
        CollectionConditionParamsJsonMapper.class,
        CollectionConditionRepositoryAdapterTest.JacksonConfig.class
})
@DisplayName("CollectionConditionRepositoryAdapter 저장 및 회사 격리")
class CollectionConditionRepositoryAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long OTHER_COMPANY_ID = 20L;

    @Autowired
    private CollectionConditionRepositoryAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                INSERT INTO crawl_source (
                    crawl_source_id, source_code, source_name,
                    source_type, enabled, created_at
                )
                VALUES (1, 'NARA', '나라장터', 'OPEN_API', true, CURRENT_TIMESTAMP)
                """);
    }

    @Test
    @DisplayName("수집 조건의 공고 유형과 필터를 JSON으로 저장하고 복원한다")
    void savesAndRestoresJsonParams() {
        CollectionCondition saved = adapter.save(condition(COMPANY_ID));

        CollectionCondition found = adapter
                .findNotDeletedById(saved.getConditionId(), COMPANY_ID)
                .orElseThrow();

        assertThat(found.getNoticeTypes())
                .containsExactly(BidNoticeType.CONSTRUCTION, BidNoticeType.SERVICE);
        assertThat(found.getFilters().keywords())
                .containsExactly("스마트시티", "통합관제");
        assertThat(found.getFilters().minimumEstimatedPrice())
                .isEqualTo(100_000_000L);
        assertThat(found.getFilters().maximumEstimatedPrice())
                .isEqualTo(1_000_000_000L);
        assertThat(found.getFilters().internationalBidType())
                .isEqualTo(InternationalBidType.DOMESTIC);
    }

    @Test
    @DisplayName("자동 수집 일정 필드를 저장하고 복원한다")
    void savesAndRestoresCollectionSchedule() {
        LocalDateTime nextRunAt = LocalDateTime.of(2026, 8, 12, 9, 0);
        CollectionCondition source = CollectionCondition.create(
                COMPANY_ID, "NARA", "평일 자동 수집",
                List.of(BidNoticeType.SERVICE), condition(COMPANY_ID).getFilters(),
                CollectionLookbackPeriod.ONE_WEEK,
                true, true, CollectionScheduleType.WEEKDAYS,
                LocalTime.of(9, 0), "Asia/Seoul", nextRunAt,
                "EMP001", LocalDateTime.of(2026, 8, 11, 10, 0)
        );

        CollectionCondition saved = adapter.save(source);
        CollectionCondition found = adapter.findNotDeletedById(
                saved.getConditionId(), COMPANY_ID
        ).orElseThrow();

        assertThat(found.isAutoCollectionEnabled()).isTrue();
        assertThat(found.getScheduleType()).isEqualTo(CollectionScheduleType.WEEKDAYS);
        assertThat(found.getScheduledTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(found.getTimezone()).isEqualTo("Asia/Seoul");
        assertThat(found.getNextRunAt()).isEqualTo(nextRunAt);
    }

    @Test
    @DisplayName("실행 시각이 된 활성 자동 수집 조건만 점유한다")
    void claimsOnlyDueAutoCollectionConditions() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 10, 0);
        CollectionCondition due = scheduledCondition(
                "실행 대상",
                now.minusMinutes(1),
                true,
                true
        );
        CollectionCondition dueExactlyNow = scheduledCondition(
                "현재 시각 실행 대상",
                now,
                true,
                true
        );
        CollectionCondition future = scheduledCondition(
                "미래 실행",
                now.plusMinutes(1),
                true,
                true
        );
        CollectionCondition disabled = scheduledCondition(
                "자동 수집 꺼짐",
                now.minusMinutes(1),
                true,
                false
        );
        CollectionCondition inactive = scheduledCondition(
                "비활성 조건",
                now.minusMinutes(1),
                false,
                true
        );
        adapter.save(due);
        adapter.save(dueExactlyNow);
        adapter.save(future);
        adapter.save(disabled);
        adapter.save(inactive);

        List<CollectionCondition> claimed =
                adapter.claimDueConditions(now, 50);

        assertThat(claimed)
                .extracting(CollectionCondition::getConditionName)
                .containsExactly("실행 대상", "현재 시각 실행 대상");
    }

    @Test
    @DisplayName("다른 회사에서는 같은 수집 조건 ID를 조회할 수 없다")
    void preventsCrossCompanyLookup() {
        CollectionCondition saved = adapter.save(condition(COMPANY_ID));

        assertThat(adapter.findNotDeletedById(
                saved.getConditionId(),
                COMPANY_ID
        )).isPresent();

        assertThat(adapter.findNotDeletedById(
                saved.getConditionId(),
                OTHER_COMPANY_ID
        )).isEmpty();
    }

    @Test
    @DisplayName("목록 조회는 현재 회사의 조건만 반환한다")
    void listsOnlyCurrentCompanyConditions() {
        adapter.save(condition(COMPANY_ID));
        adapter.save(condition(OTHER_COMPANY_ID));

        List<CollectionCondition> results =
                adapter.findAllNotDeleted(COMPANY_ID);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCompanyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    @DisplayName("논리 삭제된 수집 조건은 단건과 목록 조회에서 제외한다")
    void excludesDeletedCondition() {
        CollectionCondition saved = adapter.save(condition(COMPANY_ID));
        saved.delete(LocalDateTime.now());
        adapter.save(saved);

        assertThat(adapter.findNotDeletedById(
                saved.getConditionId(), COMPANY_ID
        )).isEmpty();
        assertThat(adapter.findAllNotDeleted(COMPANY_ID)).isEmpty();
    }

    @Test
    @DisplayName("목록은 최근 등록된 수집 조건부터 반환한다")
    void listsNewestConditionFirst() {
        CollectionCondition oldCondition = conditionAt(
                COMPANY_ID, "이전 조건", LocalDateTime.of(2026, 8, 10, 10, 0)
        );
        CollectionCondition newCondition = conditionAt(
                COMPANY_ID, "최근 조건", LocalDateTime.of(2026, 8, 10, 11, 0)
        );
        adapter.save(oldCondition);
        adapter.save(newCondition);

        assertThat(adapter.findAllNotDeleted(COMPANY_ID))
                .extracting(CollectionCondition::getConditionName)
                .containsExactly("최근 조건", "이전 조건");
    }

    private CollectionCondition condition(Long companyId) {
        return conditionAt(companyId, "수도권 스마트시티", LocalDateTime.now());
    }

    private CollectionCondition conditionAt(
            Long companyId,
            String name,
            LocalDateTime createdAt
    ) {
        CollectionConditionFilter filter = new CollectionConditionFilter(
                List.of("스마트시티", "통합관제"),
                List.of("11", "41"),
                List.of("6202"),
                100_000_000L,
                1_000_000_000L,
                true,
                InternationalBidType.DOMESTIC
        );

        return CollectionCondition.create(
                companyId,
                "NARA",
                name,
                List.of(BidNoticeType.CONSTRUCTION, BidNoticeType.SERVICE),
                filter,
                true,
                "EMP001",
                createdAt
        );
    }

    private CollectionCondition scheduledCondition(
            String name,
            LocalDateTime nextRunAt,
            boolean active,
            boolean autoCollectionEnabled
    ) {
        return CollectionCondition.create(
                COMPANY_ID,
                "NARA",
                name,
                List.of(BidNoticeType.SERVICE),
                condition(COMPANY_ID).getFilters(),
                CollectionLookbackPeriod.ONE_WEEK,
                active,
                autoCollectionEnabled,
                autoCollectionEnabled ? CollectionScheduleType.DAILY : null,
                autoCollectionEnabled ? LocalTime.of(9, 0) : null,
                autoCollectionEnabled ? "Asia/Seoul" : null,
                autoCollectionEnabled ? nextRunAt : null,
                "EMP001",
                LocalDateTime.of(2026, 8, 10, 10, 0)
        );
    }

    @TestConfiguration
    static class JacksonConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}

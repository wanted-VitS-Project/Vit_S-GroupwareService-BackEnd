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

    private CollectionCondition condition(Long companyId) {
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
                "수도권 스마트시티",
                List.of(BidNoticeType.CONSTRUCTION, BidNoticeType.SERVICE),
                filter,
                true,
                "EMP001",
                LocalDateTime.now()
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
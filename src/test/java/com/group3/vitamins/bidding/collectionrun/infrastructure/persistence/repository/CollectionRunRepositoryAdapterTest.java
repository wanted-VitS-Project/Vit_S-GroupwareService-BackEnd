package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository.CollectionConditionParamsJsonMapper;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository.CollectionConditionPersistenceMapper;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository.CollectionConditionRepositoryAdapter;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper.CollectionRunPersistenceMapper;
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
        "spring.datasource.url=jdbc:h2:mem:collection-run;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        CollectionRunRepositoryAdapter.class,
        CollectionRunPersistenceMapper.class,
        CollectionConditionRepositoryAdapter.class,
        CollectionConditionPersistenceMapper.class,
        CollectionConditionParamsJsonMapper.class,
        CollectionRunRepositoryAdapterTest.JacksonConfig.class
})
@DisplayName("CollectionRunRepositoryAdapter 저장 및 회사 격리")
class CollectionRunRepositoryAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long OTHER_COMPANY_ID = 20L;
    private static final String USER_ID = "EMP001";

    @Autowired
    private CollectionRunRepositoryAdapter runAdapter;

    @Autowired
    private CollectionConditionRepositoryAdapter conditionAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                INSERT INTO crawl_source (
                    crawl_source_id,
                    source_code,
                    source_name,
                    source_type,
                    enabled,
                    created_at
                )
                VALUES (
                    1,
                    'NARA',
                    '나라장터',
                    'OPEN_API',
                    true,
                    CURRENT_TIMESTAMP
                )
                """);
    }

    @Test
    @DisplayName("PENDING 수집 실행을 저장하고 복원한다")
    void savesAndRestoresPendingRun() {
        CollectionCondition condition =
                conditionAdapter.save(condition(COMPANY_ID));

        CollectionRun saved = runAdapter.save(
                CollectionRun.createPending(
                        condition.getConditionId(),
                        USER_ID,
                        LocalDateTime.now()
                )
        );

        CollectionRun found = runAdapter
                .findByIdAndCompanyId(
                        saved.runId(),
                        COMPANY_ID
                )
                .orElseThrow();

        assertThat(found.runId()).isNotNull();
        assertThat(found.conditionId())
                .isEqualTo(condition.getConditionId());
        assertThat(found.runStatus())
                .isEqualTo(CollectionRunStatus.PENDING);
        assertThat(found.requestedBy()).isEqualTo(USER_ID);
        assertThat(found.collectedCount()).isZero();
    }

    @Test
    @DisplayName("PENDING 실행은 진행 중인 실행으로 판단한다")
    void detectsActiveRun() {
        CollectionCondition condition =
                conditionAdapter.save(condition(COMPANY_ID));

        runAdapter.save(
                CollectionRun.createPending(
                        condition.getConditionId(),
                        USER_ID,
                        LocalDateTime.now()
                )
        );

        assertThat(runAdapter.existsActiveByConditionId(
                condition.getConditionId()
        )).isTrue();
    }

    @Test
    @DisplayName("다른 회사에서는 수집 실행을 조회할 수 없다")
    void preventsCrossCompanyLookup() {
        CollectionCondition condition =
                conditionAdapter.save(condition(COMPANY_ID));

        CollectionRun saved = runAdapter.save(
                CollectionRun.createPending(
                        condition.getConditionId(),
                        USER_ID,
                        LocalDateTime.now()
                )
        );

        assertThat(runAdapter.findByIdAndCompanyId(
                saved.runId(),
                COMPANY_ID
        )).isPresent();

        assertThat(runAdapter.findByIdAndCompanyId(
                saved.runId(),
                OTHER_COMPANY_ID
        )).isEmpty();
    }

    // 테스트에 사용할 회사별 나라장터 수집 조건을 만듭니다.
    private CollectionCondition condition(Long companyId) {
        CollectionConditionFilter filter =
                new CollectionConditionFilter(
                        List.of("스마트시티"),
                        List.of("11"),
                        List.of("6202"),
                        100_000_000L,
                        1_000_000_000L,
                        true,
                        InternationalBidType.DOMESTIC
                );

        return CollectionCondition.create(
                companyId,
                "NARA",
                "수도권 스마트시티 용역",
                List.of(BidNoticeType.SERVICE),
                filter,
                true,
                USER_ID,
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
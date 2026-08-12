package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidsummary.application.command.HandleBidNoticeSummaryCallbackCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort.BidNoticeSnapshot;
import com.group3.vitamins.bidding.bidsummary.application.service.BidNoticeSummaryCallbackService;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummary;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryOutboxJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryJpaRepository;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryOutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bid-summary-retry;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaBidNoticeSummaryWorkerAdapter.class,
        BidNoticeSummaryRetryFlowIntegrationTest.TestConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("입찰 AI 요약 재시도 DB 통합 흐름")
class BidNoticeSummaryRetryFlowIntegrationTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 12, 9, 0);
    private static final String FIRST_ATTEMPT_ID =
            "4b0f03bb-c04d-4ff0-997b-3ff762cbfe22";

    @Autowired
    private JpaBidNoticeSummaryWorkerAdapter workerAdapter;

    @Autowired
    private BidNoticeSummaryJpaRepository summaryRepository;

    @Autowired
    private BidNoticeSummaryOutboxJpaRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        summaryRepository.deleteAll();
    }

    @Test
    @DisplayName("일시 장애 후 새 attempt로 재처리하면 완료되고 이전 callback은 거절한다")
    void completesWithNewAttemptAfterTemporaryFailure() {
        Long summaryId = savePendingSummary();
        assertThat(workerAdapter.claimJob(summaryId, FIRST_ATTEMPT_ID, NOW))
                .isPresent();

        var retry = serviceAt(NOW).handle(failed(summaryId, FIRST_ATTEMPT_ID));

        BidNoticeSummaryJpaEntity pending = summaryRepository.findById(summaryId)
                .orElseThrow();
        String secondAttemptId = pending.getProcessingAttemptId();
        List<BidNoticeSummaryOutboxJpaEntity> outboxes = outboxRepository.findAll();

        assertThat(retry.accepted()).isTrue();
        assertThat(retry.summaryStatus()).isEqualTo("PENDING");
        assertThat(secondAttemptId).isNotEqualTo(FIRST_ATTEMPT_ID);
        assertThat(pending.getRetryCount()).isEqualTo(1);
        assertThat(outboxes).singleElement().satisfies(outbox -> {
            assertThat(outbox.getAttemptId()).isEqualTo(secondAttemptId);
            assertThat(outbox.getAvailableAt()).isEqualTo(NOW.plusSeconds(10));
        });

        assertThat(workerAdapter.claimJob(
                summaryId,
                secondAttemptId,
                NOW.plusSeconds(10)
        )).isPresent();

        var completed = serviceAt(NOW.plusSeconds(11))
                .handle(completed(summaryId, secondAttemptId));
        var stale = serviceAt(NOW.plusSeconds(12))
                .handle(completed(summaryId, FIRST_ATTEMPT_ID));

        BidNoticeSummaryJpaEntity saved = summaryRepository.findById(summaryId)
                .orElseThrow();
        assertThat(completed.accepted()).isTrue();
        assertThat(saved.getSummaryStatus())
                .isEqualTo(BidNoticeSummaryStatus.COMPLETED);
        assertThat(saved.getOverviewSummary()).isEqualTo("최종 공고 개요");
        assertThat(stale.accepted()).isFalse();
        assertThat(stale.reason())
                .isEqualTo("attempt_mismatch_or_already_finished");
    }

    @Test
    @DisplayName("세 번째 일시 장애는 추가 Outbox 없이 최종 실패로 종료한다")
    void failsAfterThirdTemporaryFailure() {
        Long summaryId = savePendingSummary();
        String currentAttemptId = FIRST_ATTEMPT_ID;
        LocalDateTime currentTime = NOW;

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(workerAdapter.claimJob(
                    summaryId,
                    currentAttemptId,
                    currentTime
            )).isPresent();

            var result = serviceAt(currentTime)
                    .handle(failed(summaryId, currentAttemptId));
            BidNoticeSummaryJpaEntity saved = summaryRepository.findById(summaryId)
                    .orElseThrow();

            if (attempt < 3) {
                assertThat(result.summaryStatus()).isEqualTo("PENDING");
                currentAttemptId = saved.getProcessingAttemptId();
                currentTime = attempt == 1
                        ? currentTime.plusSeconds(10)
                        : currentTime.plusSeconds(30);
            } else {
                assertThat(result.summaryStatus()).isEqualTo("FAILED");
            }
        }

        BidNoticeSummaryJpaEntity failed = summaryRepository.findById(summaryId)
                .orElseThrow();
        assertThat(failed.getSummaryStatus())
                .isEqualTo(BidNoticeSummaryStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(2);
        assertThat(failed.getErrorMessage()).isEqualTo("Gemini 일시 장애");
        assertThat(outboxRepository.findAll()).hasSize(2);
    }

    private Long savePendingSummary() {
        BidNoticeSummary summary = BidNoticeSummary.createPending(
                10L,
                20L,
                "EMP001",
                "금액과 일정을 요약해줘.",
                FIRST_ATTEMPT_ID,
                NOW.minusMinutes(1)
        );
        BidNoticeSnapshot snapshot = new BidNoticeSnapshot(
                20L,
                "스마트시티 통합관제 플랫폼 구축 용역",
                "SERVICE",
                "공고기관",
                "수요기관",
                null,
                null,
                NOW.minusDays(1),
                NOW,
                NOW.plusDays(7),
                null,
                "참가 자격",
                "지역 제한",
                "업종 제한",
                "계약 방식",
                "평가 방식",
                "https://example.org/notice",
                List.of()
        );

        return summaryRepository.saveAndFlush(
                BidNoticeSummaryJpaEntity.pending(
                        summary,
                        objectMapper.valueToTree(snapshot)
                )
        ).getSummaryId();
    }

    private BidNoticeSummaryCallbackService serviceAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(SEOUL).toInstant(), SEOUL);
        return new BidNoticeSummaryCallbackService(workerAdapter, clock);
    }

    private HandleBidNoticeSummaryCallbackCommand failed(
            Long summaryId,
            String attemptId
    ) {
        return new HandleBidNoticeSummaryCallbackCommand(
                summaryId,
                attemptId,
                "FAILED",
                null,
                null,
                null,
                null,
                null,
                null,
                "Gemini 일시 장애",
                true
        );
    }

    private HandleBidNoticeSummaryCallbackCommand completed(
            Long summaryId,
            String attemptId
    ) {
        return new HandleBidNoticeSummaryCallbackCommand(
                summaryId,
                attemptId,
                "COMPLETED",
                "최종 공고 개요",
                "금액 요약",
                "일정 요약",
                "자격 요약",
                "과업 요약",
                "위험 요약",
                null,
                false
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}

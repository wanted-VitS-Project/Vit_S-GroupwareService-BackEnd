package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort.BidNoticeSnapshot;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryWorkerPort.CompletedSummary;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummary;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryOutboxJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryJpaRepository;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryOutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("JpaBidNoticeSummaryWorkerAdapter 상태 전이")
class JpaBidNoticeSummaryWorkerAdapterTest {

    private static final Long SUMMARY_ID = 31L;
    private static final String ATTEMPT_ID = "4b0f03bb-c04d-4ff0-997b-3ff762cbfe22";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 9, 0);

    private BidNoticeSummaryJpaRepository repository;
    private BidNoticeSummaryOutboxJpaRepository outboxRepository;
    private JpaBidNoticeSummaryWorkerAdapter adapter;
    private BidNoticeSummaryJpaEntity entity;

    @BeforeEach
    void setUp() {
        repository = mock(BidNoticeSummaryJpaRepository.class);
        outboxRepository = mock(BidNoticeSummaryOutboxJpaRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        adapter = new JpaBidNoticeSummaryWorkerAdapter(
                repository,
                outboxRepository,
                objectMapper
        );
        entity = pendingEntity(objectMapper);
    }

    @Test
    @DisplayName("PENDING 작업을 PROCESSING으로 전환하고 스냅샷을 반환한다")
    void claimsPendingJob() {
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.of(entity));

        var result = adapter.claimJob(SUMMARY_ID, ATTEMPT_ID, NOW);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().notice().noticeName()).isEqualTo("테스트 공고");
        assertThat(entity.getSummaryStatus()).isEqualTo(BidNoticeSummaryStatus.PROCESSING);
        assertThat(entity.getProcessingStartedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("같은 attemptId의 PROCESSING 작업은 재조회할 수 있다")
    void returnsAlreadyClaimedJobForSameAttempt() {
        entity.startProcessing(NOW.minusSeconds(10));
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.of(entity));

        assertThat(adapter.claimJob(SUMMARY_ID, ATTEMPT_ID, NOW)).isPresent();
        assertThat(entity.getProcessingStartedAt()).isEqualTo(NOW.minusSeconds(10));
    }

    @Test
    @DisplayName("다른 attemptId는 작업을 점유할 수 없다")
    void rejectsDifferentAttempt() {
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.of(entity));

        assertThat(adapter.claimJob(
                SUMMARY_ID,
                "b8ead25a-8773-4d7d-9757-7bf5079d0d70",
                NOW
        )).isEmpty();
        assertThat(entity.getSummaryStatus()).isEqualTo(BidNoticeSummaryStatus.PENDING);
    }

    @Test
    @DisplayName("현재 attemptId의 결과를 완료하고 중복 callback은 거절한다")
    void completesOnlyOnce() {
        entity.startProcessing(NOW.minusSeconds(10));
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.of(entity));
        CompletedSummary completed = new CompletedSummary(
                "개요", "금액", "일정", "자격", "과업", "위험"
        );

        var first = adapter.complete(SUMMARY_ID, ATTEMPT_ID, completed, NOW);
        var duplicate = adapter.complete(SUMMARY_ID, ATTEMPT_ID, completed, NOW.plusSeconds(1));

        assertThat(first.accepted()).isTrue();
        assertThat(duplicate.accepted()).isFalse();
        assertThat(entity.getSummaryStatus()).isEqualTo(BidNoticeSummaryStatus.COMPLETED);
        assertThat(entity.getOverviewSummary()).isEqualTo("개요");
    }

    @Test
    @DisplayName("현재 attemptId의 실패 결과를 저장한다")
    void failsCurrentAttempt() {
        entity.startProcessing(NOW.minusSeconds(10));
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.of(entity));

        var result = adapter.fail(SUMMARY_ID, ATTEMPT_ID, "AI 호출 실패", false, NOW);

        assertThat(result.accepted()).isTrue();
        assertThat(entity.getSummaryStatus()).isEqualTo(BidNoticeSummaryStatus.FAILED);
        assertThat(entity.getErrorMessage()).isEqualTo("AI 호출 실패");
        verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("첫 일시 장애는 새 attemptId로 바꾸고 10초 뒤 Outbox를 예약한다")
    void schedulesFirstRetry() {
        entity.startProcessing(NOW.minusSeconds(10));
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.of(entity));
        ArgumentCaptor<BidNoticeSummaryOutboxJpaEntity> captor =
                ArgumentCaptor.forClass(BidNoticeSummaryOutboxJpaEntity.class);

        var result = adapter.fail(SUMMARY_ID, ATTEMPT_ID, "일시 장애", true, NOW);

        verify(outboxRepository).save(captor.capture());
        BidNoticeSummaryOutboxJpaEntity outbox = captor.getValue();
        assertThat(result.accepted()).isTrue();
        assertThat(result.currentStatus()).isEqualTo(BidNoticeSummaryStatus.PENDING);
        assertThat(entity.getRetryCount()).isEqualTo(1);
        assertThat(entity.getProcessingAttemptId()).isNotEqualTo(ATTEMPT_ID);
        assertThat(outbox.getAttemptId()).isEqualTo(entity.getProcessingAttemptId());
        assertThat(outbox.getAvailableAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(outbox.getPayload().get("retryCount").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("두 번째 일시 장애는 30초 뒤 Outbox를 예약한다")
    void schedulesSecondRetry() {
        String secondAttemptId = "be54f33d-cfee-4a17-bf48-bce42dfcb388";
        entity.startProcessing(NOW.minusMinutes(1));
        entity.prepareRetry(secondAttemptId, "첫 실패", NOW.minusSeconds(50));
        entity.startProcessing(NOW.minusSeconds(40));
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.of(entity));
        ArgumentCaptor<BidNoticeSummaryOutboxJpaEntity> captor =
                ArgumentCaptor.forClass(BidNoticeSummaryOutboxJpaEntity.class);

        var result = adapter.fail(SUMMARY_ID, secondAttemptId, "두 번째 실패", true, NOW);

        verify(outboxRepository).save(captor.capture());
        assertThat(result.currentStatus()).isEqualTo(BidNoticeSummaryStatus.PENDING);
        assertThat(entity.getRetryCount()).isEqualTo(2);
        assertThat(captor.getValue().getAvailableAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(captor.getValue().getPayload().get("retryCount").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("재시도를 모두 사용한 일시 장애는 최종 FAILED로 종료한다")
    void failsAfterRetryLimit() {
        entity.startProcessing(NOW.minusMinutes(2));
        entity.prepareRetry("be54f33d-cfee-4a17-bf48-bce42dfcb388", "첫 실패", NOW.minusMinutes(1));
        entity.startProcessing(NOW.minusSeconds(50));
        entity.prepareRetry("cbf83e43-830e-4838-8ea0-1c55bc70aa37", "두 번째 실패", NOW.minusSeconds(40));
        entity.startProcessing(NOW.minusSeconds(30));
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.of(entity));

        var result = adapter.fail(
                SUMMARY_ID,
                "cbf83e43-830e-4838-8ea0-1c55bc70aa37",
                "세 번째 실패",
                true,
                NOW
        );

        assertThat(result.accepted()).isTrue();
        assertThat(entity.getSummaryStatus()).isEqualTo(BidNoticeSummaryStatus.FAILED);
        assertThat(entity.getRetryCount()).isEqualTo(2);
        verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("존재하지 않는 요약은 exists=false를 반환한다")
    void returnsMissingResult() {
        when(repository.findForWorkerUpdate(SUMMARY_ID)).thenReturn(Optional.empty());

        var result = adapter.fail(SUMMARY_ID, ATTEMPT_ID, "실패", false, NOW);

        assertThat(result.exists()).isFalse();
        assertThat(result.accepted()).isFalse();
        assertThat(result.currentStatus()).isNull();
    }

    private BidNoticeSummaryJpaEntity pendingEntity(ObjectMapper objectMapper) {
        BidNoticeSummary summary = BidNoticeSummary.createPending(
                10L, 20L, "EMP001", "요약해줘", ATTEMPT_ID, NOW.minusMinutes(1)
        );
        BidNoticeSnapshot snapshot = new BidNoticeSnapshot(
                20L, "테스트 공고", "SERVICE", "공고기관", "수요기관",
                null, null, NOW, null, NOW.plusDays(7), null,
                "참가 자격", "지역 제한", "업종 제한", "계약 방식", "평가 방식",
                "https://example.org/notice", List.of()
        );
        BidNoticeSummaryJpaEntity result = BidNoticeSummaryJpaEntity.pending(
                summary, objectMapper.valueToTree(snapshot)
        );
        ReflectionTestUtils.setField(result, "summaryId", SUMMARY_ID);
        return result;
    }
}

package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort.BidNoticeSnapshot;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryWorkerPort;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryJobQuery;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryJobQueryService worker 작업 조회")
class BidNoticeSummaryJobQueryServiceTest {

    private static final Long SUMMARY_ID = 31L;
    private static final String ATTEMPT_ID = "4b0f03bb-c04d-4ff0-997b-3ff762cbfe22";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 9, 0);

    private BidNoticeSummaryWorkerPort workerPort;
    private BidNoticeSummaryJobQueryService service;

    @BeforeEach
    void setUp() {
        workerPort = mock(BidNoticeSummaryWorkerPort.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new BidNoticeSummaryJobQueryService(workerPort, clock);
    }

    @Test
    @DisplayName("현재 attemptId와 일치하는 작업을 점유하여 반환한다")
    void claimsCurrentJob() {
        BidNoticeSnapshot notice = mock(BidNoticeSnapshot.class);
        when(workerPort.claimJob(SUMMARY_ID, ATTEMPT_ID, NOW))
                .thenReturn(Optional.of(new BidNoticeSummaryWorkerPort.JobData(
                        SUMMARY_ID, 10L, ATTEMPT_ID, "위험 요소를 요약해줘.", null, notice
                )));

        var result = service.handle(new GetBidNoticeSummaryJobQuery(SUMMARY_ID, ATTEMPT_ID));

        assertThat(result.summaryId()).isEqualTo(SUMMARY_ID);
        assertThat(result.companyId()).isEqualTo(10L);
        assertThat(result.attemptId()).isEqualTo(ATTEMPT_ID);
        assertThat(result.previousSummary()).isNull();
        assertThat(result.notice()).isSameAs(notice);
    }

    @Test
    @DisplayName("현재 시도와 일치하는 작업이 없으면 NotFoundException을 던진다")
    void rejectsMissingJob() {
        when(workerPort.claimJob(SUMMARY_ID, ATTEMPT_ID, NOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(
                new GetBidNoticeSummaryJobQuery(SUMMARY_ID, ATTEMPT_ID)
        )).isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_SUMMARY_JOB_NOT_FOUND));
    }

    @Test
    @DisplayName("잘못된 경로 값은 Port 호출 전에 거부한다")
    void rejectsInvalidQuery() {
        assertThatThrownBy(() -> service.handle(
                new GetBidNoticeSummaryJobQuery(0L, "not-a-uuid")
        )).isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_INVALID_SUMMARY_JOB_REQUEST));

        verifyNoInteractions(workerPort);
    }

    @Test
    @DisplayName("null Query는 Port 호출 전에 거부한다")
    void rejectsNullQuery() {
        assertThatThrownBy(() -> service.handle(null))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(workerPort);
    }
}

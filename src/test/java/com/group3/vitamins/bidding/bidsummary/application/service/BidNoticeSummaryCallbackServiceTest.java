package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.command.HandleBidNoticeSummaryCallbackCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryWorkerPort;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryCallbackService worker callback")
class BidNoticeSummaryCallbackServiceTest {

    private static final Long SUMMARY_ID = 31L;
    private static final String ATTEMPT_ID = "4b0f03bb-c04d-4ff0-997b-3ff762cbfe22";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 9, 0);

    private BidNoticeSummaryWorkerPort workerPort;
    private BidNoticeSummaryCallbackService service;

    @BeforeEach
    void setUp() {
        workerPort = mock(BidNoticeSummaryWorkerPort.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new BidNoticeSummaryCallbackService(workerPort, clock);
    }

    @Test
    @DisplayName("COMPLETED callback의 요약 결과를 저장한다")
    void completesSummary() {
        when(workerPort.complete(eq(SUMMARY_ID), eq(ATTEMPT_ID), any(), eq(NOW)))
                .thenReturn(update(true, true, BidNoticeSummaryStatus.COMPLETED));

        var result = service.handle(completedCommand());

        ArgumentCaptor<BidNoticeSummaryWorkerPort.CompletedSummary> captor =
                ArgumentCaptor.forClass(BidNoticeSummaryWorkerPort.CompletedSummary.class);
        verify(workerPort).complete(eq(SUMMARY_ID), eq(ATTEMPT_ID), captor.capture(), eq(NOW));
        assertThat(captor.getValue().overviewSummary()).isEqualTo("공고 개요");
        assertThat(result.accepted()).isTrue();
        assertThat(result.summaryStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("FAILED callback의 오류 메시지를 저장한다")
    void failsSummary() {
        when(workerPort.fail(SUMMARY_ID, ATTEMPT_ID, "AI 호출 실패", false, NOW))
                .thenReturn(update(true, true, BidNoticeSummaryStatus.FAILED));

        var result = service.handle(failedCommand());

        assertThat(result.accepted()).isTrue();
        assertThat(result.summaryStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("재시도 가능한 FAILED callback은 PENDING으로 되돌린다")
    void preparesRetryableFailure() {
        when(workerPort.fail(SUMMARY_ID, ATTEMPT_ID, "일시 장애", true, NOW))
                .thenReturn(update(true, true, BidNoticeSummaryStatus.PENDING));

        var result = service.handle(failedCommand(true));

        verify(workerPort).fail(SUMMARY_ID, ATTEMPT_ID, "일시 장애", true, NOW);
        assertThat(result.accepted()).isTrue();
        assertThat(result.summaryStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("오래된 attempt callback은 멱등 거절한다")
    void ignoresStaleCallback() {
        when(workerPort.complete(eq(SUMMARY_ID), eq(ATTEMPT_ID), any(), eq(NOW)))
                .thenReturn(update(true, false, BidNoticeSummaryStatus.COMPLETED));

        var result = service.handle(completedCommand());

        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).isEqualTo("attempt_mismatch_or_already_finished");
    }

    @Test
    @DisplayName("존재하지 않는 요약 callback은 NotFoundException을 던진다")
    void rejectsMissingSummary() {
        when(workerPort.complete(eq(SUMMARY_ID), eq(ATTEMPT_ID), any(), eq(NOW)))
                .thenReturn(update(false, false, null));

        assertThatThrownBy(() -> service.handle(completedCommand()))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND));
    }

    @Test
    @DisplayName("COMPLETED에서 개요가 없으면 Port 호출 전에 거부한다")
    void rejectsCompletedWithoutOverview() {
        var command = new HandleBidNoticeSummaryCallbackCommand(
                SUMMARY_ID, ATTEMPT_ID, "COMPLETED",
                " ", null, null, null, null, null, null, false
        );

        assertInvalid(command);
    }

    @Test
    @DisplayName("FAILED에서 결과 필드가 전달되면 Port 호출 전에 거부한다")
    void rejectsFailedWithResult() {
        var command = new HandleBidNoticeSummaryCallbackCommand(
                SUMMARY_ID, ATTEMPT_ID, "FAILED",
                "결과가 있으면 안 됨", null, null, null, null, null, "AI 호출 실패", false
        );

        assertInvalid(command);
    }

    private void assertInvalid(HandleBidNoticeSummaryCallbackCommand command) {
        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_INVALID_SUMMARY_CALLBACK));
        verifyNoInteractions(workerPort);
    }

    private HandleBidNoticeSummaryCallbackCommand completedCommand() {
        return new HandleBidNoticeSummaryCallbackCommand(
                SUMMARY_ID, ATTEMPT_ID, "COMPLETED",
                "공고 개요", "금액 요약", "일정 요약", "자격 요약", "과업 요약", "위험 요약", null, false
        );
    }

    private HandleBidNoticeSummaryCallbackCommand failedCommand() {
        return failedCommand(false);
    }

    private HandleBidNoticeSummaryCallbackCommand failedCommand(boolean retryable) {
        return new HandleBidNoticeSummaryCallbackCommand(
                SUMMARY_ID, ATTEMPT_ID, "FAILED",
                null, null, null, null, null, null,
                retryable ? "일시 장애" : "AI 호출 실패",
                retryable
        );
    }

    private BidNoticeSummaryWorkerPort.CallbackUpdate update(
            boolean exists,
            boolean accepted,
            BidNoticeSummaryStatus status
    ) {
        return new BidNoticeSummaryWorkerPort.CallbackUpdate(exists, accepted, status);
    }
}

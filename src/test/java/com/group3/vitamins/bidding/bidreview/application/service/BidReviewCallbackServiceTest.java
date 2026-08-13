package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.command.HandleBidReviewCallbackCommand;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewWorkerPort;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("BidReviewCallbackService worker callback")
class BidReviewCallbackServiceTest {

    private static final Long REVIEW_ID = 71L;
    private static final String ATTEMPT_ID = "4b0f03bb-c04d-4ff0-997b-3ff762cbfe22";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 9, 0);

    private BidReviewWorkerPort workerPort;
    private BidReviewCallbackService service;

    @BeforeEach
    void setUp() {
        workerPort = mock(BidReviewWorkerPort.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-13T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new BidReviewCallbackService(workerPort, clock);
    }

    @Test
    @DisplayName("PROCESSING callback은 문서별 진행상황만 갱신한다")
    void reportsProgress() {
        when(workerPort.reportProgress(eq(REVIEW_ID), eq(ATTEMPT_ID), any(), eq(NOW)))
                .thenReturn(update(true, true, "PROCESSING", null));

        var result = service.handle(processingCommand());

        ArgumentCaptor<List<BidReviewWorkerPort.DocumentOutcome>> captor = ArgumentCaptor.forClass(List.class);
        verify(workerPort).reportProgress(eq(REVIEW_ID), eq(ATTEMPT_ID), captor.capture(), eq(NOW));
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).bidAttachmentId()).isEqualTo(31L);
        assertThat(result.accepted()).isTrue();
        assertThat(result.reviewStatus()).isEqualTo("PROCESSING");
        verify(workerPort, never()).complete(any(), any(), any(), any(), any(), any());
        verify(workerPort, never()).fail(any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("COMPLETED callback의 결과와 근거를 저장한다")
    void completesReview() {
        when(workerPort.complete(eq(REVIEW_ID), eq(ATTEMPT_ID), any(), any(), any(), eq(NOW)))
                .thenReturn(update(true, true, "COMPLETED", null));

        var result = service.handle(completedCommand());

        ArgumentCaptor<List<BidReviewWorkerPort.CitationInput>> citationCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(workerPort).complete(
                eq(REVIEW_ID), eq(ATTEMPT_ID), eq("검토 결과"), any(), citationCaptor.capture(), eq(NOW)
        );
        assertThat(citationCaptor.getValue()).hasSize(1);
        assertThat(citationCaptor.getValue().get(0).excerpt()).isEqualTo("발췌문");
        assertThat(result.accepted()).isTrue();
        assertThat(result.reviewStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("재시도 불가능한 FAILED callback을 최종 실패로 저장한다")
    void failsReview() {
        when(workerPort.fail(eq(REVIEW_ID), eq(ATTEMPT_ID), eq("UNSUPPORTED_FORMAT"), eq("지원하지 않는 형식"), eq(false), any(), eq(NOW)))
                .thenReturn(update(true, true, "FAILED", null));

        var result = service.handle(failedCommand(false));

        assertThat(result.accepted()).isTrue();
        assertThat(result.reviewStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("재시도 가능한 FAILED callback은 retryable=true로 전달한다")
    void requestsRetry() {
        when(workerPort.fail(eq(REVIEW_ID), eq(ATTEMPT_ID), eq("DOWNLOAD_TIMEOUT"), eq("다운로드 시간 초과"), eq(true), any(), eq(NOW)))
                .thenReturn(update(true, true, "PENDING", null));

        var result = service.handle(failedCommand(true));

        verify(workerPort).fail(eq(REVIEW_ID), eq(ATTEMPT_ID), eq("DOWNLOAD_TIMEOUT"), eq("다운로드 시간 초과"), eq(true), any(), eq(NOW));
        assertThat(result.accepted()).isTrue();
        assertThat(result.reviewStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("오래된 attempt callback은 멱등 거절한다")
    void ignoresStaleCallback() {
        when(workerPort.complete(eq(REVIEW_ID), eq(ATTEMPT_ID), any(), any(), any(), eq(NOW)))
                .thenReturn(update(true, false, "COMPLETED", null));

        var result = service.handle(completedCommand());

        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).isEqualTo("attempt_mismatch_or_already_finished");
    }

    @Test
    @DisplayName("존재하지 않는 검토 callback은 NotFoundException을 던진다")
    void rejectsMissingReview() {
        when(workerPort.complete(eq(REVIEW_ID), eq(ATTEMPT_ID), any(), any(), any(), eq(NOW)))
                .thenReturn(update(false, false, null, null));

        assertThatThrownBy(() -> service.handle(completedCommand()))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_NOT_FOUND));
    }

    @Test
    @DisplayName("COMPLETED에서 result가 없으면 Port 호출 전에 거부한다")
    void rejectsCompletedWithoutResult() {
        var command = new HandleBidReviewCallbackCommand(
                REVIEW_ID, ATTEMPT_ID, "COMPLETED",
                " ", null, null, false, null, null
        );

        assertInvalid(command);
    }

    @Test
    @DisplayName("FAILED에서 result가 함께 오면 Port 호출 전에 거부한다")
    void rejectsFailedWithResult() {
        var command = new HandleBidReviewCallbackCommand(
                REVIEW_ID, ATTEMPT_ID, "FAILED",
                "결과가 있으면 안 됨", "ERR", "실패 메시지", false, null, null
        );

        assertInvalid(command);
    }

    @Test
    @DisplayName("PROCESSING에서 citations가 오면 Port 호출 전에 거부한다")
    void rejectsProcessingWithCitations() {
        var command = new HandleBidReviewCallbackCommand(
                REVIEW_ID, ATTEMPT_ID, "PROCESSING",
                null, null, null, false, null,
                List.of(new HandleBidReviewCallbackCommand.CitationInputCommand(
                        1, "BID_ATTACHMENT", 31L, null, null, "제안요청서.pdf", 3, null, "발췌문"
                ))
        );

        assertInvalid(command);
    }

    private void assertInvalid(HandleBidReviewCallbackCommand command) {
        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_INVALID_REVIEW_CALLBACK));
        verifyNoInteractions(workerPort);
    }

    private HandleBidReviewCallbackCommand processingCommand() {
        return new HandleBidReviewCallbackCommand(
                REVIEW_ID, ATTEMPT_ID, "PROCESSING",
                null, null, null, false,
                List.of(new HandleBidReviewCallbackCommand.DocumentOutcomeInput(
                        31L, "READY", "tmp/reviews/71/31.pdf", 204800L, "application/pdf"
                )),
                null
        );
    }

    private HandleBidReviewCallbackCommand completedCommand() {
        return new HandleBidReviewCallbackCommand(
                REVIEW_ID, ATTEMPT_ID, "COMPLETED",
                "검토 결과", null, null, false,
                null,
                List.of(new HandleBidReviewCallbackCommand.CitationInputCommand(
                        1, "BID_ATTACHMENT", 31L, null, null, "제안요청서.pdf", 3, null, "발췌문"
                ))
        );
    }

    private HandleBidReviewCallbackCommand failedCommand(boolean retryable) {
        return new HandleBidReviewCallbackCommand(
                REVIEW_ID, ATTEMPT_ID, "FAILED",
                null,
                retryable ? "DOWNLOAD_TIMEOUT" : "UNSUPPORTED_FORMAT",
                retryable ? "다운로드 시간 초과" : "지원하지 않는 형식",
                retryable,
                null,
                null
        );
    }

    private BidReviewWorkerPort.CallbackUpdate update(
            boolean exists,
            boolean accepted,
            String currentStatus,
            String reason
    ) {
        return new BidReviewWorkerPort.CallbackUpdate(exists, accepted, currentStatus, reason);
    }
}

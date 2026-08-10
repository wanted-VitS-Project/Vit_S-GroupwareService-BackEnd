package com.group3.vitamins.vitamate.filecleanup.application.service;

import com.group3.vitamins.vitamate.filecleanup.application.command.HandleVitamateCleanupCallbackCommand;
import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupJob;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupJobStorePort;
import com.group3.vitamins.vitamate.filecleanup.application.result.VitamateCleanupCallbackResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VitamateCleanupCallbackService")
class VitamateCleanupCallbackServiceTest {

    private static final Long CLEANUP_JOB_ID = 31L;
    private static final String ATTEMPT_ID = "91f3c9c4-27dd-48e7-af1b-732b69eac214";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 18, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-09T09:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private VitamateCleanupJobStorePort cleanupJobStorePort;

    private VitamateCleanupCallbackService callbackService;

    @BeforeEach
    void setUp() {
        callbackService = new VitamateCleanupCallbackService(cleanupJobStorePort, FIXED_CLOCK, 5);
    }

    @Test
    @DisplayName("4번째 재시도 가능 실패는 5분 후 다음 처리를 예약한다")
    void schedulesLastRetryAfterFourthFailure() {
        when(cleanupJobStorePort.findAttemptCount(CLEANUP_JOB_ID)).thenReturn(Optional.of(4));
        when(cleanupJobStorePort.scheduleRetry(
                CLEANUP_JOB_ID,
                ATTEMPT_ID,
                "CHROMA_UNAVAILABLE",
                "ChromaDB is temporarily unavailable",
                5,
                NOW.plusMinutes(5),
                NOW
        )).thenReturn(true);

        VitamateCleanupCallbackResult result = callbackService.handle(failed(true));

        assertThat(result.accepted()).isTrue();
        assertThat(result.cleanupStatus()).isEqualTo("RETRY_WAIT");
        verify(cleanupJobStorePort, never()).markDeadLetter(
                CLEANUP_JOB_ID, ATTEMPT_ID, "CHROMA_UNAVAILABLE",
                "ChromaDB is temporarily unavailable", NOW
        );
    }

    @Test
    @DisplayName("5번째 재시도 가능 실패는 DEAD_LETTER로 마감한다")
    void deadLettersAfterFifthFailure() {
        when(cleanupJobStorePort.findAttemptCount(CLEANUP_JOB_ID)).thenReturn(Optional.of(5));
        when(cleanupJobStorePort.markDeadLetter(
                CLEANUP_JOB_ID,
                ATTEMPT_ID,
                "CHROMA_UNAVAILABLE",
                "ChromaDB is temporarily unavailable",
                NOW
        )).thenReturn(true);

        VitamateCleanupCallbackResult result = callbackService.handle(failed(true));

        assertThat(result.accepted()).isTrue();
        assertThat(result.cleanupStatus()).isEqualTo("DEAD_LETTER");
        verify(cleanupJobStorePort, never()).scheduleRetry(
                CLEANUP_JOB_ID,
                ATTEMPT_ID,
                "CHROMA_UNAVAILABLE",
                "ChromaDB is temporarily unavailable",
                5,
                NOW.plusMinutes(5),
                NOW
        );
    }

    @Test
    @DisplayName("재시도 불가능한 실패는 첫 시도에도 DEAD_LETTER로 마감한다")
    void deadLettersNonRetryableFailureImmediately() {
        when(cleanupJobStorePort.findAttemptCount(CLEANUP_JOB_ID)).thenReturn(Optional.of(1));
        when(cleanupJobStorePort.markDeadLetter(
                CLEANUP_JOB_ID,
                ATTEMPT_ID,
                "INVALID_VECTOR_ID",
                "Vector identifier is invalid",
                NOW
        )).thenReturn(true);

        VitamateCleanupCallbackResult result = callbackService.handle(new HandleVitamateCleanupCallbackCommand(
                CLEANUP_JOB_ID,
                ATTEMPT_ID,
                "FAILED",
                false,
                null,
                "INVALID_VECTOR_ID",
                "Vector identifier is invalid"
        ));

        assertThat(result.accepted()).isTrue();
        assertThat(result.cleanupStatus()).isEqualTo("DEAD_LETTER");
    }

    @Test
    @DisplayName("오래된 attempt의 완료 callback은 현재 상태를 유지하고 거절한다")
    void rejectsStaleCompletedCallback() {
        when(cleanupJobStorePort.findAttemptCount(CLEANUP_JOB_ID)).thenReturn(Optional.of(2));
        when(cleanupJobStorePort.markCompleted(CLEANUP_JOB_ID, ATTEMPT_ID, 3, NOW)).thenReturn(false);
        when(cleanupJobStorePort.findStatus(CLEANUP_JOB_ID))
                .thenReturn(Optional.of(VitamateCleanupJob.Status.PROCESSING));

        VitamateCleanupCallbackResult result = callbackService.handle(new HandleVitamateCleanupCallbackCommand(
                CLEANUP_JOB_ID,
                ATTEMPT_ID,
                "COMPLETED",
                false,
                3,
                null,
                null
        ));

        assertThat(result.accepted()).isFalse();
        assertThat(result.cleanupStatus()).isEqualTo("PROCESSING");
        assertThat(result.reason()).isEqualTo("attempt_mismatch_or_already_finished");
    }

    private HandleVitamateCleanupCallbackCommand failed(boolean retryable) {
        return new HandleVitamateCleanupCallbackCommand(
                CLEANUP_JOB_ID,
                ATTEMPT_ID,
                "FAILED",
                retryable,
                null,
                "CHROMA_UNAVAILABLE",
                "ChromaDB is temporarily unavailable"
        );
    }
}

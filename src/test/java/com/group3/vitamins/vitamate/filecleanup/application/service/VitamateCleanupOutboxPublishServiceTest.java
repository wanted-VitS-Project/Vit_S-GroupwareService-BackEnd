package com.group3.vitamins.vitamate.filecleanup.application.service;

import com.group3.vitamins.vitamate.filecleanup.application.model.ClaimedVitamateCleanupOutbox;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupJobPublisherPort;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupOutboxStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VitamateCleanupOutboxPublishService")
class VitamateCleanupOutboxPublishServiceTest {

    private static final String LOCK_OWNER = "spring-instance-1";
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 9, 17, 0);

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-09T08:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private VitamateCleanupOutboxStorePort outboxStorePort;

    @Mock
    private VitamateCleanupJobPublisherPort jobPublisherPort;

    private VitamateCleanupOutboxPublishService publishService;

    @BeforeEach
    void setUp() {
        publishService = new VitamateCleanupOutboxPublishService(
                outboxStorePort,
                jobPublisherPort,
                FIXED_CLOCK
        );
    }

    @Test
    @DisplayName("Redis 발행에 성공하면 Outbox를 PUBLISHED 처리한다")
    void marksOutboxPublishedAfterRedisSuccess() {
        ClaimedVitamateCleanupOutbox outbox = outbox(1L, 1);

        when(outboxStorePort.claimPublishable(
                LOCK_OWNER,
                10,
                NOW,
                NOW.plusSeconds(30)
        )).thenReturn(List.of(outbox));

        int publishedCount =
                publishService.publishBatch(LOCK_OWNER, 10);

        assertThat(publishedCount).isEqualTo(1);

        verify(jobPublisherPort).publish(outbox);
        verify(outboxStorePort).markPublished(
                outbox.outboxId(),
                LOCK_OWNER,
                NOW
        );
        verify(outboxStorePort, never()).markPublishFailed(
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Redis 발행에 실패하면 재시도 가능한 상태로 저장한다")
    void schedulesRetryAfterRedisFailure() {
        ClaimedVitamateCleanupOutbox outbox = outbox(1L, 1);

        when(outboxStorePort.claimPublishable(
                LOCK_OWNER,
                10,
                NOW,
                NOW.plusSeconds(30)
        )).thenReturn(List.of(outbox));

        doThrow(new IllegalStateException("Redis unavailable"))
                .when(jobPublisherPort)
                .publish(outbox);

        int publishedCount =
                publishService.publishBatch(LOCK_OWNER, 10);

        assertThat(publishedCount).isZero();

        verify(outboxStorePort).markPublishFailed(
                outbox.outboxId(),
                LOCK_OWNER,
                "IllegalStateException",
                NOW.plusSeconds(10)
        );
        verify(outboxStorePort, never()).markPublished(
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("한 작업이 실패해도 다음 Outbox 발행을 계속한다")
    void continuesPublishingAfterSingleFailure() {
        ClaimedVitamateCleanupOutbox failedOutbox = outbox(1L, 1);
        ClaimedVitamateCleanupOutbox successfulOutbox = outbox(2L, 1);

        when(outboxStorePort.claimPublishable(
                LOCK_OWNER,
                10,
                NOW,
                NOW.plusSeconds(30)
        )).thenReturn(List.of(failedOutbox, successfulOutbox));

        doThrow(new IllegalStateException("Redis unavailable"))
                .when(jobPublisherPort)
                .publish(failedOutbox);

        int publishedCount =
                publishService.publishBatch(LOCK_OWNER, 10);

        assertThat(publishedCount).isEqualTo(1);

        verify(jobPublisherPort).publish(failedOutbox);
        verify(jobPublisherPort).publish(successfulOutbox);

        verify(outboxStorePort).markPublishFailed(
                failedOutbox.outboxId(),
                LOCK_OWNER,
                "IllegalStateException",
                NOW.plusSeconds(10)
        );
        verify(outboxStorePort).markPublished(
                successfulOutbox.outboxId(),
                LOCK_OWNER,
                NOW
        );
    }

    @ParameterizedTest(name = "발행 시도 {0}회이면 {1}초 후 재시도한다")
    @CsvSource({
            "1, 10",
            "2, 30",
            "3, 60",
            "4, 300",
            "10, 300"
    })
    @DisplayName("발행 실패 횟수에 따라 재시도 간격을 늘린다")
    void appliesRetryDelay(
            int publishAttemptCount,
            long expectedDelaySeconds
    ) {
        ClaimedVitamateCleanupOutbox outbox =
                outbox(1L, publishAttemptCount);

        when(outboxStorePort.claimPublishable(
                LOCK_OWNER,
                10,
                NOW,
                NOW.plusSeconds(30)
        )).thenReturn(List.of(outbox));

        doThrow(new IllegalStateException("Redis unavailable"))
                .when(jobPublisherPort)
                .publish(outbox);

        publishService.publishBatch(LOCK_OWNER, 10);

        ArgumentCaptor<LocalDateTime> nextRetryCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(outboxStorePort).markPublishFailed(
                eq(outbox.outboxId()),
                eq(LOCK_OWNER),
                eq("IllegalStateException"),
                nextRetryCaptor.capture()
        );

        assertThat(nextRetryCaptor.getValue())
                .isEqualTo(NOW.plusSeconds(expectedDelaySeconds));
    }

    @Test
    @DisplayName("lockOwner가 비어 있으면 발행을 시작하지 않는다")
    void rejectsBlankLockOwner() {
        assertThatThrownBy(() ->
                publishService.publishBatch(" ", 10)
        ).isInstanceOf(IllegalArgumentException.class);

        verify(outboxStorePort, never()).claimPublishable(
                any(),
                any(Integer.class),
                any(),
                any()
        );
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "101"})
    @DisplayName("batchSize가 허용 범위를 벗어나면 발행을 시작하지 않는다")
    void rejectsInvalidBatchSize(int batchSize) {
        assertThatThrownBy(() ->
                publishService.publishBatch(LOCK_OWNER, batchSize)
        ).isInstanceOf(IllegalArgumentException.class);

        verify(outboxStorePort, never()).claimPublishable(
                any(),
                any(Integer.class),
                any(),
                any()
        );
    }

    // 테스트마다 필요한 점유 완료 Outbox를 생성합니다.
    private ClaimedVitamateCleanupOutbox outbox(
            Long outboxId,
            int publishAttemptCount
    ) {
        return new ClaimedVitamateCleanupOutbox(
                outboxId,
                1000L + outboxId,
                "event-" + outboxId,
                "CHROMA_VECTOR_DELETE_REQUESTED",
                "cleanup-key-" + outboxId,
                "attempt-" + outboxId,
                List.of(910001L, 910002L),
                0,
                publishAttemptCount
        );
    }
}
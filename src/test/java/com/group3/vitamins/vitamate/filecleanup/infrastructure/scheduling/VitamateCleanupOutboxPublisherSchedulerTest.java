package com.group3.vitamins.vitamate.filecleanup.infrastructure.scheduling;

import com.group3.vitamins.vitamate.filecleanup.application.service.VitamateCleanupOutboxPublishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VitamateCleanupOutboxPublisherScheduler")
class VitamateCleanupOutboxPublisherSchedulerTest {

    private static final int BATCH_SIZE = 50;

    @Mock
    private VitamateCleanupOutboxPublishService publishService;

    private com.group3.vitamins.vitamate.filecleanup.infrastructure.scheduling.VitamateCleanupOutboxPublisherScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler =
                new com.group3.vitamins.vitamate.filecleanup.infrastructure.scheduling.VitamateCleanupOutboxPublisherScheduler(
                        publishService
                );

        // Spring Context 없이 @Value 설정값을 주입합니다.
        ReflectionTestUtils.setField(
                scheduler,
                "batchSize",
                BATCH_SIZE
        );
    }

    @Test
    @DisplayName("설정된 배치 크기로 미발행 Outbox 처리를 요청한다")
    void publishesPendingOutboxesWithConfiguredBatchSize() {
        when(publishService.publishBatch(
                anyString(),
                eq(BATCH_SIZE)
        )).thenReturn(2);

        scheduler.publishPendingOutboxes();

        ArgumentCaptor<String> lockOwnerCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(publishService).publishBatch(
                lockOwnerCaptor.capture(),
                eq(BATCH_SIZE)
        );

        assertThat(lockOwnerCaptor.getValue())
                .startsWith("vitamate-cleanup-");
    }

    @Test
    @DisplayName("발행할 Outbox가 없어도 정상 종료한다")
    void completesWhenNoOutboxIsAvailable() {
        when(publishService.publishBatch(
                anyString(),
                eq(BATCH_SIZE)
        )).thenReturn(0);

        assertThatCode(() ->
                scheduler.publishPendingOutboxes()
        ).doesNotThrowAnyException();

        verify(publishService).publishBatch(
                anyString(),
                eq(BATCH_SIZE)
        );
    }

    @Test
    @DisplayName("발행 배치에서 오류가 발생해도 Scheduler 실행을 종료하지 않는다")
    void isolatesPublishBatchFailure() {
        doThrow(new IllegalStateException("DB unavailable"))
                .when(publishService)
                .publishBatch(
                        anyString(),
                        eq(BATCH_SIZE)
                );

        assertThatCode(() ->
                scheduler.publishPendingOutboxes()
        ).doesNotThrowAnyException();

        verify(publishService).publishBatch(
                anyString(),
                eq(BATCH_SIZE)
        );
    }
}
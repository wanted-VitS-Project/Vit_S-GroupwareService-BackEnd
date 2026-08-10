package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskFailure;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionRunTaskFailureService")
class CollectionRunTaskFailureServiceTest {

    @Mock
    private CollectionRunTaskPort taskPort;

    @Mock
    private CollectionRunOutboxStorePort outboxStorePort;

    @Mock
    private CollectionRunTaskFailure failure;

    @Test
    @DisplayName("Task 실패 전이가 성공하면 같은 흐름에서 DLQ Outbox를 저장한다")
    void savesDlqOutboxAfterTaskFailure() {
        LocalDateTime failedAt = LocalDateTime.of(2026, 8, 10, 23, 40);
        when(failure.taskId()).thenReturn(10L);
        when(failure.attemptId()).thenReturn("attempt-1");
        when(taskPort.fail(
                10L, "attempt-1", "TIMEOUT", "TIMEOUT", failedAt
        )).thenReturn(true);
        CollectionRunTaskFailureService service = new CollectionRunTaskFailureService(
                taskPort, outboxStorePort);

        boolean recorded = service.recordPermanentFailure(
                failure, "TIMEOUT", "TIMEOUT", failedAt);

        assertThat(recorded).isTrue();
        ArgumentCaptor<String> eventId = ArgumentCaptor.forClass(String.class);
        verify(outboxStorePort).saveTaskFailurePending(
                eventId.capture(), org.mockito.ArgumentMatchers.same(failure),
                org.mockito.ArgumentMatchers.eq(failedAt));
        assertThat(eventId.getValue()).isNotBlank();
    }

    @Test
    @DisplayName("attemptId가 달라 Task 실패 전이가 거부되면 Outbox를 저장하지 않는다")
    void doesNotSaveOutboxWhenTaskFailureIsRejected() {
        LocalDateTime failedAt = LocalDateTime.of(2026, 8, 10, 23, 40);
        when(failure.taskId()).thenReturn(10L);
        when(failure.attemptId()).thenReturn("stale-attempt");
        when(taskPort.fail(
                10L, "stale-attempt", "TIMEOUT", "TIMEOUT", failedAt
        )).thenReturn(false);
        CollectionRunTaskFailureService service = new CollectionRunTaskFailureService(
                taskPort, outboxStorePort);

        boolean recorded = service.recordPermanentFailure(
                failure, "TIMEOUT", "TIMEOUT", failedAt);

        assertThat(recorded).isFalse();
        verify(outboxStorePort, never()).saveTaskFailurePending(
                anyString(),
                org.mockito.ArgumentMatchers.any(CollectionRunTaskFailure.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }
}

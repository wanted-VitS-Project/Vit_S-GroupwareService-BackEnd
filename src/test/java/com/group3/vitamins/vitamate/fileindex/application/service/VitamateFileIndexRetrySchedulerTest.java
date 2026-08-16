package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexJobPublisherPort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexJobPublisherPort.FileIndexJob;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort.ReclaimResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VitamateFileIndexRetryScheduler")
class VitamateFileIndexRetrySchedulerTest {

    private static final Long FILE_VERSION_ID_1 = 900001L;
    private static final Long FILE_VERSION_ID_2 = 900002L;
    private static final String NEW_ATTEMPT_ID_1 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String NEW_ATTEMPT_ID_2 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    private VitamateFileIndexStorePort fileIndexStorePort;
    private VitamateFileIndexJobPublisherPort jobPublisherPort;
    private VitamateFileIndexRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        fileIndexStorePort = mock(VitamateFileIndexStorePort.class);
        jobPublisherPort = mock(VitamateFileIndexJobPublisherPort.class);
        scheduler = new VitamateFileIndexRetryScheduler(fileIndexStorePort, jobPublisherPort);

        when(fileIndexStorePort.findExhaustedFileVersionIdCandidates(any(), anyInt())).thenReturn(List.of());
        when(fileIndexStorePort.findReclaimableFileVersionIdCandidates(any(), anyInt())).thenReturn(List.of());
    }

    @Test
    @DisplayName("claim에 성공한 후보만 재발행한다")
    void publishesOnlyClaimedCandidates() {
        when(fileIndexStorePort.findReclaimableFileVersionIdCandidates(any(), anyInt()))
                .thenReturn(List.of(FILE_VERSION_ID_1));
        when(fileIndexStorePort.claimForRetry(eq(FILE_VERSION_ID_1), any()))
                .thenReturn(new ReclaimResult(true, NEW_ATTEMPT_ID_1));

        scheduler.retryStalePendingJobs();

        verify(jobPublisherPort).publish(argThat((FileIndexJob job) -> job.fileVersionId().equals(FILE_VERSION_ID_1)));
    }

    @Test
    @DisplayName("claim에 실패한 후보는 재발행하지 않는다")
    void doesNotPublishWhenNotClaimed() {
        when(fileIndexStorePort.findReclaimableFileVersionIdCandidates(any(), anyInt()))
                .thenReturn(List.of(FILE_VERSION_ID_1));
        when(fileIndexStorePort.claimForRetry(eq(FILE_VERSION_ID_1), any()))
                .thenReturn(ReclaimResult.notClaimed());

        scheduler.retryStalePendingJobs();

        verify(jobPublisherPort, never()).publish(any());
    }

    @Test
    @DisplayName("한 항목의 발행 실패가 나머지 후보 처리를 막지 않고, 소모된 retry_count를 보상한다")
    void isolatesPublishFailurePerItemAndCompensates() {
        when(fileIndexStorePort.findReclaimableFileVersionIdCandidates(any(), anyInt()))
                .thenReturn(List.of(FILE_VERSION_ID_1, FILE_VERSION_ID_2));
        when(fileIndexStorePort.claimForRetry(eq(FILE_VERSION_ID_1), any()))
                .thenReturn(new ReclaimResult(true, NEW_ATTEMPT_ID_1));
        when(fileIndexStorePort.claimForRetry(eq(FILE_VERSION_ID_2), any()))
                .thenReturn(new ReclaimResult(true, NEW_ATTEMPT_ID_2));
        doThrow(new RuntimeException("redis down"))
                .when(jobPublisherPort).publish(argThat((FileIndexJob job) -> job.fileVersionId().equals(FILE_VERSION_ID_1)));

        scheduler.retryStalePendingJobs();

        // 첫 번째 항목의 발행 실패로 소모된 retry_count를 되돌린다.
        verify(fileIndexStorePort).compensatePublishFailure(eq(FILE_VERSION_ID_1), eq(NEW_ATTEMPT_ID_1), any());
        // 두 번째 항목은 첫 번째 실패와 무관하게 정상적으로 claim·발행된다.
        verify(fileIndexStorePort).claimForRetry(eq(FILE_VERSION_ID_2), any());
        verify(jobPublisherPort).publish(argThat((FileIndexJob job) -> job.fileVersionId().equals(FILE_VERSION_ID_2)));
        verify(fileIndexStorePort, never()).compensatePublishFailure(eq(FILE_VERSION_ID_2), any(), any());
    }

    @Test
    @DisplayName("재시도 상한을 소진한 후보는 재발행 대신 종료 처리한다")
    void failsExhaustedCandidatesInstead() {
        when(fileIndexStorePort.findExhaustedFileVersionIdCandidates(any(), anyInt()))
                .thenReturn(List.of(FILE_VERSION_ID_1));

        scheduler.retryStalePendingJobs();

        verify(fileIndexStorePort, times(1)).failExhausted(eq(FILE_VERSION_ID_1), any(), any());
        verify(fileIndexStorePort, never()).claimForRetry(eq(FILE_VERSION_ID_1), any());
    }
}

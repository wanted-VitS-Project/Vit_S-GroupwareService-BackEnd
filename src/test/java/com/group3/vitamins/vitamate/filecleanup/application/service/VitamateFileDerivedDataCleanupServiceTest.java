package com.group3.vitamins.vitamate.filecleanup.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.filecleanup.application.command.CleanupVitamateFileDerivedDataCommand;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupJobStorePort;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateFileDerivedDataCleanupPort;
import com.group3.vitamins.vitamate.filecleanup.application.result.CleanupVitamateFileDerivedDataResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("VitamateFileDerivedDataCleanupService")
class VitamateFileDerivedDataCleanupServiceTest {

    private static final Long FILE_ID = 900001L;
    private VitamateCleanupJobStorePort cleanupJobStorePort;
    private VitamateFileDerivedDataCleanupPort cleanupPort;
    private VitamateFileDerivedDataCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupPort = mock(VitamateFileDerivedDataCleanupPort.class);
        cleanupJobStorePort = mock(VitamateCleanupJobStorePort.class);

        cleanupService = new VitamateFileDerivedDataCleanupService(
                cleanupPort,
                cleanupJobStorePort
        );
    }

    @Nested
    @DisplayName("cleanup")
    class Cleanup {

        @Test
        @DisplayName("delegates fileId based cleanup to port")
        void delegatesCleanupToPort() {
            CleanupVitamateFileDerivedDataResult expected = new CleanupVitamateFileDerivedDataResult(
                    FILE_ID,
                    1,
                    1,
                    1,
                    1
            );
            when(cleanupPort.cleanupByFileId(FILE_ID)).thenReturn(expected);

            CleanupVitamateFileDerivedDataResult result = cleanupService.handle(
                    new CleanupVitamateFileDerivedDataCommand(FILE_ID)
            );
            InOrder inOrder = inOrder(cleanupJobStorePort, cleanupPort);
            inOrder.verify(cleanupJobStorePort).createCleanupJob(FILE_ID);
            inOrder.verify(cleanupPort).cleanupByFileId(FILE_ID);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("cleanup job 생성이 실패하면 파생 데이터 삭제를 시작하지 않는다")
        void stopsCleanupWhenJobCreationFails() {
            doThrow(new IllegalStateException("cleanup job creation failed"))
                    .when(cleanupJobStorePort)
                    .createCleanupJob(FILE_ID);

            assertThatThrownBy(() -> cleanupService.handle(
                    new CleanupVitamateFileDerivedDataCommand(FILE_ID)
            )).isInstanceOf(IllegalStateException.class);

            verify(cleanupPort, never()).cleanupByFileId(FILE_ID);
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects null command")
        void rejectsNullCommand() {
            assertInvalid(null);
        }

        @Test
        @DisplayName("rejects missing fileId")
        void rejectsMissingFileId() {
            assertInvalid(new CleanupVitamateFileDerivedDataCommand(null));
        }

        @Test
        @DisplayName("rejects zero fileId")
        void rejectsZeroFileId() {
            assertInvalid(new CleanupVitamateFileDerivedDataCommand(0L));
        }

        @Test
        @DisplayName("rejects negative fileId")
        void rejectsNegativeFileId() {
            assertInvalid(new CleanupVitamateFileDerivedDataCommand(-1L));
        }

        private void assertInvalid(CleanupVitamateFileDerivedDataCommand command) {
            assertThatThrownBy(() -> cleanupService.handle(command))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verifyNoInteractions(cleanupJobStorePort, cleanupPort);
        }
    }
}

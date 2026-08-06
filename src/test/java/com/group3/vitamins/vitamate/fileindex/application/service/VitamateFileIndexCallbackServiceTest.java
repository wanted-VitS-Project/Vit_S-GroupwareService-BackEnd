package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.HandleVitamateFileIndexCallbackCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexCallbackResult;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("VitamateFileIndexCallbackService")
class VitamateFileIndexCallbackServiceTest {

    private static final Long FILE_VERSION_ID = 900001L;

    private VitamateFileIndexStorePort fileIndexStore;
    private VitamateFileIndexCallbackService callbackService;

    @BeforeEach
    void setUp() {
        fileIndexStore = mock(VitamateFileIndexStorePort.class);
        callbackService = new VitamateFileIndexCallbackService(fileIndexStore);
    }

    @Nested
    @DisplayName("status callback save")
    class SaveCallback {

        @Test
        @DisplayName("saves PENDING status")
        void savesPendingStatus() {
            when(fileIndexStore.existsFileVersion(FILE_VERSION_ID)).thenReturn(true);
            when(fileIndexStore.upsertStatus(eq(FILE_VERSION_ID), eq(FileIndexStatus.PENDING), eq(null), any()))
                    .thenReturn(FileIndexStatus.PENDING);

            VitamateFileIndexCallbackResult result = callbackService.handle(command("PENDING", null));

            assertThat(result.accepted()).isTrue();
            assertThat(result.indexStatus()).isEqualTo("PENDING");
            verify(fileIndexStore).upsertStatus(eq(FILE_VERSION_ID), eq(FileIndexStatus.PENDING), eq(null), any());
        }

        @Test
        @DisplayName("saves PROCESSING status")
        void savesProcessingStatus() {
            when(fileIndexStore.existsFileVersion(FILE_VERSION_ID)).thenReturn(true);
            when(fileIndexStore.upsertStatus(eq(FILE_VERSION_ID), eq(FileIndexStatus.PROCESSING), eq(null), any()))
                    .thenReturn(FileIndexStatus.PROCESSING);

            VitamateFileIndexCallbackResult result = callbackService.handle(command("PROCESSING", null));

            assertThat(result.accepted()).isTrue();
            assertThat(result.fileVersionId()).isEqualTo(FILE_VERSION_ID);
            assertThat(result.indexStatus()).isEqualTo("PROCESSING");
            assertThat(result.reason()).isNull();
            verify(fileIndexStore).upsertStatus(eq(FILE_VERSION_ID), eq(FileIndexStatus.PROCESSING), eq(null), any());
        }

        @Test
        @DisplayName("saves COMPLETED status without errorMessage")
        void savesCompletedStatus() {
            when(fileIndexStore.existsFileVersion(FILE_VERSION_ID)).thenReturn(true);
            when(fileIndexStore.upsertStatus(eq(FILE_VERSION_ID), eq(FileIndexStatus.COMPLETED), eq(null), any()))
                    .thenReturn(FileIndexStatus.COMPLETED);

            VitamateFileIndexCallbackResult result = callbackService.handle(command("COMPLETED", null));

            assertThat(result.accepted()).isTrue();
            assertThat(result.indexStatus()).isEqualTo("COMPLETED");
            verify(fileIndexStore).upsertStatus(eq(FILE_VERSION_ID), eq(FileIndexStatus.COMPLETED), eq(null), any());
        }

        @Test
        @DisplayName("saves FAILED status with errorMessage")
        void savesFailedStatusWithErrorMessage() {
            when(fileIndexStore.existsFileVersion(FILE_VERSION_ID)).thenReturn(true);
            when(fileIndexStore.upsertStatus(eq(FILE_VERSION_ID), eq(FileIndexStatus.FAILED), eq("extract failed"), any()))
                    .thenReturn(FileIndexStatus.FAILED);

            VitamateFileIndexCallbackResult result = callbackService.handle(command("FAILED", "extract failed"));

            assertThat(result.accepted()).isTrue();
            assertThat(result.indexStatus()).isEqualTo("FAILED");
            verify(fileIndexStore).upsertStatus(eq(FILE_VERSION_ID), eq(FileIndexStatus.FAILED), eq("extract failed"), any());
        }

        @Test
        @DisplayName("rejects missing file version with 404 error")
        void throwsNotFoundWhenFileVersionDoesNotExist() {
            when(fileIndexStore.existsFileVersion(FILE_VERSION_ID)).thenReturn(false);

            assertThatThrownBy(() -> callbackService.handle(command("PROCESSING", null)))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND));

            verify(fileIndexStore, never()).upsertStatus(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("input validation")
    class ValidateInput {

        @Test
        @DisplayName("rejects unknown status")
        void rejectsUnknownStatus() {
            assertInvalid(command("DONE", null));
        }

        @Test
        @DisplayName("rejects FAILED without errorMessage")
        void rejectsFailedWithoutErrorMessage() {
            assertInvalid(command("FAILED", " "));
        }

        @Test
        @DisplayName("rejects errorMessage for non FAILED status")
        void rejectsErrorMessageForNonFailedStatus() {
            assertInvalid(command("COMPLETED", "should be empty"));
        }

        @Test
        @DisplayName("rejects errorMessage for PENDING status")
        void rejectsErrorMessageForPendingStatus() {
            assertInvalid(command("PENDING", "should be empty"));
        }

        @Test
        @DisplayName("rejects missing fileVersionId before store access")
        void rejectsMissingFileVersionId() {
            assertInvalid(new HandleVitamateFileIndexCallbackCommand(null, "PROCESSING", null));
        }

        @Test
        @DisplayName("rejects zero fileVersionId before store access")
        void rejectsZeroFileVersionId() {
            assertInvalid(new HandleVitamateFileIndexCallbackCommand(0L, "PROCESSING", null));
        }

        @Test
        @DisplayName("rejects negative fileVersionId before store access")
        void rejectsNegativeFileVersionId() {
            assertInvalid(new HandleVitamateFileIndexCallbackCommand(-1L, "PROCESSING", null));
        }

        private void assertInvalid(HandleVitamateFileIndexCallbackCommand command) {
            assertThatThrownBy(() -> callbackService.handle(command))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verifyNoInteractions(fileIndexStore);
        }
    }

    // Builds callback command fixtures in one place.
    private HandleVitamateFileIndexCallbackCommand command(String status, String errorMessage) {
        return new HandleVitamateFileIndexCallbackCommand(FILE_VERSION_ID, status, errorMessage);
    }
}

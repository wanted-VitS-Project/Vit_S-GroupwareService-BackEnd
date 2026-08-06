package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateChunkEmbeddingsCommand;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateChunkEmbeddingsCommand.ChunkEmbeddingCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateChunkEmbeddingsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("VitamateChunkEmbeddingSaveService")
class VitamateChunkEmbeddingSaveServiceTest {

    private static final Long FILE_VERSION_ID = 900001L;
    private static final String INDEX_ATTEMPT_ID = "550e8400-e29b-41d4-a716-446655440000";

    private VitamateFileIndexDataPort fileIndexDataPort;
    private VitamateChunkEmbeddingSaveService embeddingSaveService;

    @BeforeEach
    void setUp() {
        fileIndexDataPort = mock(VitamateFileIndexDataPort.class);
        embeddingSaveService = new VitamateChunkEmbeddingSaveService(fileIndexDataPort);
    }

    @Nested
    @DisplayName("chunk embedding save")
    class SaveEmbeddings {

        @Test
        @DisplayName("updates chunk embedding result after file version check")
        void updatesChunkEmbeddingResult() {
            SaveVitamateChunkEmbeddingsCommand command = command(List.of(chunk(100L, "vitamate:document-chunk:100")));
            when(fileIndexDataPort.existsIndexableFileVersionForUpdate(FILE_VERSION_ID)).thenReturn(true);
            when(fileIndexDataPort.updateChunkEmbeddings(
                    eq(FILE_VERSION_ID),
                    eq(INDEX_ATTEMPT_ID),
                    eq("gemini-embedding-001"),
                    any()
            )).thenReturn(1);

            SaveVitamateChunkEmbeddingsResult result = embeddingSaveService.handle(command);

            assertThat(result.fileVersionId()).isEqualTo(FILE_VERSION_ID);
            assertThat(result.indexAttemptId()).isEqualTo(INDEX_ATTEMPT_ID);
            assertThat(result.updatedChunkCount()).isEqualTo(1);
            assertThat(result.embeddingStatus()).isEqualTo("COMPLETED");
            verify(fileIndexDataPort).existsIndexableFileVersionForUpdate(FILE_VERSION_ID);
            verify(fileIndexDataPort).updateChunkEmbeddings(eq(FILE_VERSION_ID), eq(INDEX_ATTEMPT_ID), eq("gemini-embedding-001"), any());
        }

        @Test
        @DisplayName("throws not found when file version source is missing")
        void throwsNotFoundWhenFileVersionSourceIsMissing() {
            SaveVitamateChunkEmbeddingsCommand command = command(List.of(chunk(100L, "vitamate:document-chunk:100")));
            when(fileIndexDataPort.existsIndexableFileVersionForUpdate(FILE_VERSION_ID)).thenReturn(false);

            assertThatThrownBy(() -> embeddingSaveService.handle(command))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND));

            verify(fileIndexDataPort, never()).updateChunkEmbeddings(any(), any(), any(), any());
        }

        @Test
        @DisplayName("throws not found when any chunk does not belong to file version")
        void throwsNotFoundWhenChunkBelongsToAnotherFileVersion() {
            SaveVitamateChunkEmbeddingsCommand command = command(List.of(chunk(100L, "vitamate:document-chunk:100")));
            when(fileIndexDataPort.existsIndexableFileVersionForUpdate(FILE_VERSION_ID)).thenReturn(true);
            when(fileIndexDataPort.updateChunkEmbeddings(eq(FILE_VERSION_ID), eq(INDEX_ATTEMPT_ID), eq("gemini-embedding-001"), any()))
                    .thenReturn(0);

            assertThatThrownBy(() -> embeddingSaveService.handle(command))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("input validation")
    class ValidateInput {

        @Test
        @DisplayName("rejects null command")
        void rejectsNullCommand() {
            assertInvalid(null);
        }

        @Test
        @DisplayName("rejects missing fileVersionId")
        void rejectsMissingFileVersionId() {
            assertInvalid(new SaveVitamateChunkEmbeddingsCommand(
                    null,
                    INDEX_ATTEMPT_ID,
                    "gemini-embedding-001",
                    List.of(chunk(1L, "chroma-1"))
            ));
        }

        @Test
        @DisplayName("rejects blank indexAttemptId")
        void rejectsBlankIndexAttemptId() {
            assertInvalid(new SaveVitamateChunkEmbeddingsCommand(
                    FILE_VERSION_ID,
                    " ",
                    "gemini-embedding-001",
                    List.of(chunk(1L, "chroma-1"))
            ));
        }

        @Test
        @DisplayName("rejects blank embedding model")
        void rejectsBlankEmbeddingModel() {
            assertInvalid(new SaveVitamateChunkEmbeddingsCommand(
                    FILE_VERSION_ID,
                    INDEX_ATTEMPT_ID,
                    " ",
                    List.of(chunk(1L, "chroma-1"))
            ));
        }

        @Test
        @DisplayName("rejects too long embedding model")
        void rejectsTooLongEmbeddingModel() {
            assertInvalid(new SaveVitamateChunkEmbeddingsCommand(
                    FILE_VERSION_ID,
                    INDEX_ATTEMPT_ID,
                    "m".repeat(101),
                    List.of(chunk(1L, "chroma-1"))
            ));
        }

        @Test
        @DisplayName("rejects empty chunks")
        void rejectsEmptyChunks() {
            assertInvalid(command(List.of()));
        }

        @Test
        @DisplayName("rejects too many chunks")
        void rejectsTooManyChunks() {
            List<ChunkEmbeddingCommand> chunks = IntStream.rangeClosed(1, 501)
                    .mapToObj(index -> chunk((long) index, "chroma-" + index))
                    .toList();

            assertInvalid(command(chunks));
        }

        @Test
        @DisplayName("rejects missing documentChunkId")
        void rejectsMissingDocumentChunkId() {
            assertInvalid(command(List.of(chunk(null, "chroma-1"))));
        }

        @Test
        @DisplayName("rejects blank chromaId")
        void rejectsBlankChromaId() {
            assertInvalid(command(List.of(chunk(1L, " "))));
        }

        @Test
        @DisplayName("rejects duplicate documentChunkId")
        void rejectsDuplicateDocumentChunkId() {
            assertInvalid(command(List.of(
                    chunk(1L, "chroma-1"),
                    chunk(1L, "chroma-2")
            )));
        }

        @Test
        @DisplayName("rejects duplicate chromaId")
        void rejectsDuplicateChromaId() {
            assertInvalid(command(List.of(
                    chunk(1L, "chroma-1"),
                    chunk(2L, "chroma-1")
            )));
        }

        private void assertInvalid(SaveVitamateChunkEmbeddingsCommand command) {
            assertThatThrownBy(() -> embeddingSaveService.handle(command))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verifyNoInteractions(fileIndexDataPort);
        }
    }

    // 테스트용 임베딩 저장 command를 만듭니다.
    private SaveVitamateChunkEmbeddingsCommand command(List<ChunkEmbeddingCommand> chunks) {
        return new SaveVitamateChunkEmbeddingsCommand(
                FILE_VERSION_ID,
                INDEX_ATTEMPT_ID,
                "gemini-embedding-001",
                chunks
        );
    }

    // Spring chunk와 Chroma vector를 연결하는 테스트 값을 만듭니다.
    private ChunkEmbeddingCommand chunk(Long documentChunkId, String chromaId) {
        return new ChunkEmbeddingCommand(documentChunkId, chromaId);
    }
}

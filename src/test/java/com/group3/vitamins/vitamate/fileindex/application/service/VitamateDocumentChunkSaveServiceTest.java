package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand.ChunkCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.SavedDocumentChunk;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.SavedDocumentChunks;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("VitamateDocumentChunkSaveService")
class VitamateDocumentChunkSaveServiceTest {

    private static final Long FILE_VERSION_ID = 900001L;
    private static final String INDEX_ATTEMPT_ID = "550e8400-e29b-41d4-a716-446655440000";

    private VitamateFileIndexDataPort fileIndexDataPort;
    private VitamateDocumentChunkSaveService saveService;

    @BeforeEach
    void setUp() {
        fileIndexDataPort = mock(VitamateFileIndexDataPort.class);
        saveService = new VitamateDocumentChunkSaveService(fileIndexDataPort);
    }

    @Nested
    @DisplayName("document chunk save")
    class SaveChunks {

        @Test
        @DisplayName("saves extracted chunks and returns saved chunk ids")
        void savesExtractedChunks() {
            SaveVitamateDocumentChunksCommand command = command(List.of(chunk(0, "첫 번째 청크")));
            when(fileIndexDataPort.existsIndexableFileVersionForUpdate(FILE_VERSION_ID)).thenReturn(true);
            when(fileIndexDataPort.replaceChunks(eq(FILE_VERSION_ID), eq(command.chunks())))
                    .thenReturn(new SavedDocumentChunks(
                            INDEX_ATTEMPT_ID,
                            List.of(new SavedDocumentChunk(100L, 0, "PENDING"))
                    ));

            SaveVitamateDocumentChunksResult result = saveService.handle(command);

            assertThat(result.fileVersionId()).isEqualTo(FILE_VERSION_ID);
            assertThat(result.indexAttemptId()).isEqualTo(INDEX_ATTEMPT_ID);
            assertThat(result.savedChunkCount()).isEqualTo(1);
            assertThat(result.savedChunks()).hasSize(1);
            assertThat(result.savedChunks().get(0).documentChunkId()).isEqualTo(100L);
            assertThat(result.savedChunks().get(0).chunkIndex()).isEqualTo(0);
            assertThat(result.savedChunks().get(0).embeddingStatus()).isEqualTo("PENDING");
            verify(fileIndexDataPort).existsIndexableFileVersionForUpdate(FILE_VERSION_ID);
            verify(fileIndexDataPort).replaceChunks(FILE_VERSION_ID, command.chunks());
        }

        @Test
        @DisplayName("throws not found when file version source is missing")
        void throwsNotFoundWhenFileVersionSourceIsMissing() {
            SaveVitamateDocumentChunksCommand command = command(List.of(chunk(0, "첫 번째 청크")));
            when(fileIndexDataPort.existsIndexableFileVersionForUpdate(FILE_VERSION_ID)).thenReturn(false);

            assertThatThrownBy(() -> saveService.handle(command))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND));

            verify(fileIndexDataPort, never()).replaceChunks(any(), any());
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
            assertInvalid(new SaveVitamateDocumentChunksCommand(null, List.of(chunk(0, "청크"))));
        }

        @Test
        @DisplayName("rejects empty chunk list")
        void rejectsEmptyChunkList() {
            assertInvalid(command(List.of()));
        }

        @Test
        @DisplayName("rejects duplicate chunkIndex")
        void rejectsDuplicateChunkIndex() {
            assertInvalid(command(List.of(
                    chunk(0, "첫 번째 청크"),
                    chunk(0, "중복 청크")
            )));
        }

        @Test
        @DisplayName("rejects blank excerpt")
        void rejectsBlankExcerpt() {
            assertInvalid(command(List.of(chunk(0, " "))));
        }

        @Test
        @DisplayName("rejects excerpt longer than document_chunk limit")
        void rejectsTooLongExcerpt() {
            assertInvalid(command(List.of(chunk(0, "가".repeat(1001)))));
        }

        @Test
        @DisplayName("rejects too many chunks")
        void rejectsTooManyChunks() {
            List<ChunkCommand> chunks = IntStream.rangeClosed(0, 500)
                    .mapToObj(index -> chunk(index, "청크 " + index))
                    .toList();

            assertInvalid(command(chunks));
        }

        @Test
        @DisplayName("rejects invalid offset range")
        void rejectsInvalidOffsetRange() {
            assertInvalid(command(List.of(new ChunkCommand(
                    0,
                    1,
                    "섹션",
                    10,
                    5,
                    3,
                    "청크"
            ))));
        }

        private void assertInvalid(SaveVitamateDocumentChunksCommand command) {
            assertThatThrownBy(() -> saveService.handle(command))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verifyNoInteractions(fileIndexDataPort);
        }
    }

    // 테스트용 저장 command를 한 곳에서 만듭니다.
    private SaveVitamateDocumentChunksCommand command(List<ChunkCommand> chunks) {
        return new SaveVitamateDocumentChunksCommand(FILE_VERSION_ID, chunks);
    }

    // document_chunk 한 행에 해당하는 테스트 chunk를 만듭니다.
    private ChunkCommand chunk(Integer chunkIndex, String excerpt) {
        return new ChunkCommand(
                chunkIndex,
                1,
                "테스트 섹션",
                0,
                80,
                30,
                excerpt
        );
    }
}

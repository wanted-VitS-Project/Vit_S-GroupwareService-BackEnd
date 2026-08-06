package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand.ChunkCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
        @DisplayName("saves extracted chunks after file version check")
        void savesExtractedChunks() {
            SaveVitamateDocumentChunksCommand command = command(List.of(chunk(0, "첫 번째 청크")));
            when(fileIndexDataPort.findIndexSource(FILE_VERSION_ID)).thenReturn(Optional.of(sourceResult()));
            when(fileIndexDataPort.replaceChunks(eq(FILE_VERSION_ID), eq(command.chunks()))).thenReturn(1);

            SaveVitamateDocumentChunksResult result = saveService.handle(command);

            assertThat(result.fileVersionId()).isEqualTo(FILE_VERSION_ID);
            assertThat(result.savedChunkCount()).isEqualTo(1);
            verify(fileIndexDataPort).findIndexSource(FILE_VERSION_ID);
            verify(fileIndexDataPort).replaceChunks(FILE_VERSION_ID, command.chunks());
        }

        @Test
        @DisplayName("throws not found when file version source is missing")
        void throwsNotFoundWhenFileVersionSourceIsMissing() {
            SaveVitamateDocumentChunksCommand command = command(List.of(chunk(0, "첫 번째 청크")));
            when(fileIndexDataPort.findIndexSource(FILE_VERSION_ID)).thenReturn(Optional.empty());

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

    // 테스트용 청크 저장 command를 만든다.
    private SaveVitamateDocumentChunksCommand command(List<ChunkCommand> chunks) {
        return new SaveVitamateDocumentChunksCommand(FILE_VERSION_ID, chunks);
    }

    // document_chunk 한 행에 해당하는 테스트 청크를 만든다.
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

    // 파일 버전 존재 검증에 사용할 인덱싱 소스 결과를 만든다.
    private VitamateFileIndexSourceResult sourceResult() {
        return new VitamateFileIndexSourceResult(
                FILE_VERSION_ID,
                900001L,
                900001L,
                "proposal.pdf",
                "pdf",
                "application/pdf",
                1024L,
                "local/vitamate/proposal.pdf",
                "https://example.com/download"
        );
    }
}

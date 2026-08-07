package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort;
import com.group3.vitamins.vitamate.fileindex.application.query.GetVitamateFileIndexSourceQuery;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("VitamateFileIndexSourceQueryService")
class VitamateFileIndexSourceQueryServiceTest {

    private static final Long FILE_VERSION_ID = 900001L;

    private VitamateFileIndexDataPort fileIndexDataPort;
    private VitamateFileIndexSourceQueryService queryService;

    @BeforeEach
    void setUp() {
        fileIndexDataPort = mock(VitamateFileIndexDataPort.class);
        queryService = new VitamateFileIndexSourceQueryService(fileIndexDataPort);
    }

    @Nested
    @DisplayName("index source query")
    class QueryIndexSource {

        @Test
        @DisplayName("returns file index source when file version exists")
        void returnsFileIndexSource() {
            VitamateFileIndexSourceResult source = sourceResult();
            when(fileIndexDataPort.findIndexSource(FILE_VERSION_ID)).thenReturn(Optional.of(source));

            VitamateFileIndexSourceResult result = queryService.handle(new GetVitamateFileIndexSourceQuery(FILE_VERSION_ID));

            assertThat(result).isEqualTo(source);
            verify(fileIndexDataPort).findIndexSource(FILE_VERSION_ID);
        }

        @Test
        @DisplayName("throws not found when file version cannot be indexed")
        void throwsNotFoundWhenSourceDoesNotExist() {
            when(fileIndexDataPort.findIndexSource(FILE_VERSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.handle(new GetVitamateFileIndexSourceQuery(FILE_VERSION_ID)))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("input validation")
    class ValidateInput {

        @Test
        @DisplayName("rejects null query")
        void rejectsNullQuery() {
            assertInvalid(null);
        }

        @Test
        @DisplayName("rejects missing fileVersionId")
        void rejectsMissingFileVersionId() {
            assertInvalid(new GetVitamateFileIndexSourceQuery(null));
        }

        @Test
        @DisplayName("rejects non-positive fileVersionId")
        void rejectsNonPositiveFileVersionId() {
            assertInvalid(new GetVitamateFileIndexSourceQuery(0L));
            assertInvalid(new GetVitamateFileIndexSourceQuery(-1L));
        }

        private void assertInvalid(GetVitamateFileIndexSourceQuery query) {
            assertThatThrownBy(() -> queryService.handle(query))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            if (query == null || query.fileVersionId() == null || query.fileVersionId() <= 0) {
                verifyNoInteractions(fileIndexDataPort);
            } else {
                verify(fileIndexDataPort, never()).findIndexSource(query.fileVersionId());
            }
        }
    }

    // 테스트에서 사용할 파일 인덱싱 소스 결과를 만든다.
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

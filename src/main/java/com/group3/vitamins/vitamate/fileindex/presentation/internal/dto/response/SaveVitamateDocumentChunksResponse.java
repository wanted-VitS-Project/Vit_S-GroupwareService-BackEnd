package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;
import io.swagger.v3.oas.annotations.media.Schema;

// document_chunk 저장 결과 응답
@Schema(description = "document_chunk 저장 결과 응답")
public record SaveVitamateDocumentChunksResponse(
        @Schema(description = "파일 버전 ID", example = "101")
        Long fileVersionId,
        @Schema(description = "저장된 청크 수", example = "1")
        int savedChunkCount
) {
    public static SaveVitamateDocumentChunksResponse from(SaveVitamateDocumentChunksResult result) {
        return new SaveVitamateDocumentChunksResponse(
                result.fileVersionId(),
                result.savedChunkCount()
        );
    }
}

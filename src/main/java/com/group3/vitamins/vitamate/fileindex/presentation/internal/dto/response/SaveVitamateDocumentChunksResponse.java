package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;

// document_chunk 저장 결과 응답
public record SaveVitamateDocumentChunksResponse(
        Long fileVersionId,
        int savedChunkCount
) {
    public static SaveVitamateDocumentChunksResponse from(SaveVitamateDocumentChunksResult result) {
        return new SaveVitamateDocumentChunksResponse(
                result.fileVersionId(),
                result.savedChunkCount()
        );
    }
}
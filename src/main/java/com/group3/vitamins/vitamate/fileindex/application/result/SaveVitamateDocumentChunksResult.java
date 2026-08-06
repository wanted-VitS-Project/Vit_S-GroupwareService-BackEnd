package com.group3.vitamins.vitamate.fileindex.application.result;

// document_chunk 저장 결과
public record SaveVitamateDocumentChunksResult(
        Long fileVersionId,
        int savedChunkCount
) {
}
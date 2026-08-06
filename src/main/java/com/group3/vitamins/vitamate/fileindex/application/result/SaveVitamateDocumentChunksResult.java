package com.group3.vitamins.vitamate.fileindex.application.result;

import java.util.List;

// document_chunk 저장 결과
public record SaveVitamateDocumentChunksResult(
        Long fileVersionId,
        int savedChunkCount,
        List<SavedChunkResult> savedChunks
) {

    // Python worker가 ChromaDB 저장에 사용할 document_chunk 식별자입니다.
    public record SavedChunkResult(
            Long documentChunkId,
            Integer chunkIndex,
            String embeddingStatus
    ) {
    }
}

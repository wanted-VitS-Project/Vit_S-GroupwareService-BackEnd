package com.group3.vitamins.vitamate.fileindex.application.result;

// document_chunk 임베딩 결과 반영 결과입니다.
public record SaveVitamateChunkEmbeddingsResult(
        Long fileVersionId,
        int updatedChunkCount,
        String embeddingStatus
) {
}

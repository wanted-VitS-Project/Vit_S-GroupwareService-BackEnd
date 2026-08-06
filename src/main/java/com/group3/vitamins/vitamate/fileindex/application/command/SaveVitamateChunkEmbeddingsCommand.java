package com.group3.vitamins.vitamate.fileindex.application.command;

import java.util.List;

// Python worker가 ChromaDB 저장 후 Spring DB에 반영할 임베딩 결과 command입니다.
public record SaveVitamateChunkEmbeddingsCommand(
        Long fileVersionId,
        String indexAttemptId,
        String embeddingModel,
        List<ChunkEmbeddingCommand> chunks
) {

    // document_chunk와 ChromaDB vector를 연결하는 최소 정보입니다.
    public record ChunkEmbeddingCommand(
            Long documentChunkId,
            String chromaId
    ) {
    }
}

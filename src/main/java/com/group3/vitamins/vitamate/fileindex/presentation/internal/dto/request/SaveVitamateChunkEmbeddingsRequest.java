package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.request;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateChunkEmbeddingsCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Python worker가 ChromaDB 저장 후 전달하는 임베딩 결과 요청입니다.
@Schema(description = "document_chunk 임베딩 결과 저장 요청")
public record SaveVitamateChunkEmbeddingsRequest(
        @Schema(description = "임베딩 생성에 사용한 모델명", example = "gemini-embedding-001")
        String embeddingModel,

        @Schema(description = "임베딩 결과를 반영할 chunk 목록")
        List<ChunkEmbeddingRequest> chunks
) {

    public SaveVitamateChunkEmbeddingsCommand toCommand(Long fileVersionId) {
        List<SaveVitamateChunkEmbeddingsCommand.ChunkEmbeddingCommand> chunkCommands = chunks == null
                ? null
                : chunks.stream()
                .map(chunk -> chunk == null ? null : chunk.toCommand())
                .toList();

        return new SaveVitamateChunkEmbeddingsCommand(
                fileVersionId,
                embeddingModel,
                chunkCommands
        );
    }

    // Spring DB chunk와 ChromaDB vector를 연결하는 요청 요소입니다.
    @Schema(description = "chunk 임베딩 결과")
    public record ChunkEmbeddingRequest(
            @Schema(description = "Spring DB의 document_chunk ID", example = "9001")
            Long documentChunkId,

            @Schema(description = "ChromaDB에 저장된 vector ID", example = "vitamate:document-chunk:9001")
            String chromaId
    ) {

        public SaveVitamateChunkEmbeddingsCommand.ChunkEmbeddingCommand toCommand() {
            return new SaveVitamateChunkEmbeddingsCommand.ChunkEmbeddingCommand(
                    documentChunkId,
                    chromaId
            );
        }
    }
}

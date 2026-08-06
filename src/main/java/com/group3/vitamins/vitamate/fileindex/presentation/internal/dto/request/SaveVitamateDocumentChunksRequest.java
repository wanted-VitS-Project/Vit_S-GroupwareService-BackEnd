package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.request;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;

import java.util.List;

// Python worker가 추출한 문서 청크 저장 요청
public record SaveVitamateDocumentChunksRequest(
        List<ChunkRequest> chunks
) {
    public SaveVitamateDocumentChunksCommand toCommand(Long fileVersionId) {
        List<SaveVitamateDocumentChunksCommand.ChunkCommand> chunkCommands = chunks == null
                ? null
                : chunks.stream()
                .map(chunk -> chunk == null ? null : chunk.toCommand())
                .toList();

        return new SaveVitamateDocumentChunksCommand(fileVersionId, chunkCommands);
    }

    // document_chunk 한 행으로 저장할 청크 요청값
    public record ChunkRequest(
            Integer chunkIndex,
            Integer pageNumber,
            String sectionTitle,
            Integer startOffset,
            Integer endOffset,
            Integer tokenCount,
            String excerpt
    ) {
        public SaveVitamateDocumentChunksCommand.ChunkCommand toCommand() {
            return new SaveVitamateDocumentChunksCommand.ChunkCommand(
                    chunkIndex,
                    pageNumber,
                    sectionTitle,
                    startOffset,
                    endOffset,
                    tokenCount,
                    excerpt
            );
        }
    }
}
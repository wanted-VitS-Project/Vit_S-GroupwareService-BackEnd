package com.group3.vitamins.vitamate.fileindex.application.command;

import java.util.List;

// Python worker가 추출한 document_chunk 목록 저장 command
public record SaveVitamateDocumentChunksCommand(
        Long fileVersionId,
        List<ChunkCommand> chunks
) {

    // document_chunk 한 행으로 저장할 청크 정보
    public record ChunkCommand(
            Integer chunkIndex,
            Integer pageNumber,
            String sectionTitle,
            Integer startOffset,
            Integer endOffset,
            Integer tokenCount,
            String excerpt
    ) {
    }
}
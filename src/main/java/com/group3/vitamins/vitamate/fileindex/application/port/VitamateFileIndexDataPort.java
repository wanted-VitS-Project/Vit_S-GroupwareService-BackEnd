package com.group3.vitamins.vitamate.fileindex.application.port;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;

import java.util.List;
import java.util.Optional;

// 파일 인덱싱에 필요한 파일 정보 조회와 document_chunk 저장을 담당하는 포트
public interface VitamateFileIndexDataPort {

    Optional<VitamateFileIndexSourceResult> findIndexSource(Long fileVersionId);

    boolean existsIndexableFileVersionForUpdate(Long fileVersionId);

    SavedDocumentChunks replaceChunks(Long fileVersionId, List<SaveVitamateDocumentChunksCommand.ChunkCommand> chunks);

    int updateChunkEmbeddings(
            Long fileVersionId,
            String indexAttemptId,
            String embeddingModel,
            List<ChunkEmbedding> chunks
    );

    // 이번 파일 인덱싱 시도와 저장된 chunk 목록을 함께 돌려줍니다.
    record SavedDocumentChunks(
            String indexAttemptId,
            List<SavedDocumentChunk> chunks
    ) {
    }

    // Spring DB에 저장된 document_chunk의 최소 식별 정보입니다.
    record SavedDocumentChunk(
            Long documentChunkId,
            Integer chunkIndex,
            String embeddingStatus
    ) {
    }

    // ChromaDB 저장 후 Spring DB에 반영할 chunk 임베딩 식별 정보입니다.
    record ChunkEmbedding(
            Long documentChunkId,
            String chromaId
    ) {
    }
}

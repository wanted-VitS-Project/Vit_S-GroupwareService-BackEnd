package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// document_chunk 저장 결과 응답 DTO입니다.
@Schema(description = "document_chunk 저장 결과 응답")
public record SaveVitamateDocumentChunksResponse(
        @Schema(description = "파일 버전 ID", example = "101")
        Long fileVersionId,

        @Schema(description = "이번 파일 인덱싱 시도 ID. 이후 임베딩 저장과 callback 요청에 그대로 전달한다", example = "550e8400-e29b-41d4-a716-446655440000")
        String indexAttemptId,

        @Schema(description = "저장된 chunk 수", example = "1")
        int savedChunkCount,

        @Schema(description = "저장된 document_chunk 목록")
        List<SavedChunkResponse> savedChunks
) {

    // application result를 Python worker용 HTTP 응답으로 변환합니다.
    public static SaveVitamateDocumentChunksResponse from(SaveVitamateDocumentChunksResult result) {
        return new SaveVitamateDocumentChunksResponse(
                result.fileVersionId(),
                result.indexAttemptId(),
                result.savedChunkCount(),
                result.savedChunks().stream()
                        .map(SavedChunkResponse::from)
                        .toList()
        );
    }

    // Python worker가 ChromaDB 저장에 사용할 chunk 식별자입니다.
    @Schema(description = "저장된 document_chunk 정보")
    public record SavedChunkResponse(
            @Schema(description = "Spring DB의 document_chunk ID", example = "9001")
            Long documentChunkId,

            @Schema(description = "파일 버전 안에서의 chunk 순서", example = "0")
            Integer chunkIndex,

            @Schema(description = "chunk 저장 직후 임베딩 상태", example = "PENDING")
            String embeddingStatus
    ) {

        public static SavedChunkResponse from(SaveVitamateDocumentChunksResult.SavedChunkResult result) {
            return new SavedChunkResponse(
                    result.documentChunkId(),
                    result.chunkIndex(),
                    result.embeddingStatus()
            );
        }
    }
}

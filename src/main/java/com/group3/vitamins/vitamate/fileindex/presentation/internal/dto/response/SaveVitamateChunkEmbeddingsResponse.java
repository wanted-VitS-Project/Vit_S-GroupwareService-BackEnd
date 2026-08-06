package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateChunkEmbeddingsResult;
import io.swagger.v3.oas.annotations.media.Schema;

// document_chunk 임베딩 결과 반영 응답 DTO입니다.
@Schema(description = "document_chunk 임베딩 결과 저장 응답")
public record SaveVitamateChunkEmbeddingsResponse(
        @Schema(description = "파일 버전 ID", example = "101")
        Long fileVersionId,

        @Schema(description = "임베딩 결과가 반영된 chunk 수", example = "2")
        int updatedChunkCount,

        @Schema(description = "최종 임베딩 상태", example = "COMPLETED")
        String embeddingStatus
) {

    public static SaveVitamateChunkEmbeddingsResponse from(SaveVitamateChunkEmbeddingsResult result) {
        return new SaveVitamateChunkEmbeddingsResponse(
                result.fileVersionId(),
                result.updatedChunkCount(),
                result.embeddingStatus()
        );
    }
}

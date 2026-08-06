package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.request;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Python worker가 추출한 문서 청크 저장 요청
@Schema(description = "Python worker가 추출한 문서 청크 저장 요청")
public record SaveVitamateDocumentChunksRequest(
        @Schema(description = "저장할 문서 청크 목록. 1개 이상 500개 이하", requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Schema(description = "document_chunk 한 행으로 저장할 청크 요청값")
    public record ChunkRequest(
            @Schema(description = "파일 버전 내 청크 순서. 0부터 시작", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
            Integer chunkIndex,
            @Schema(description = "페이지 번호. 알 수 없으면 null", example = "1")
            Integer pageNumber,
            @Schema(description = "섹션 제목. 알 수 없으면 null", example = "제안 개요")
            String sectionTitle,
            @Schema(description = "원문 시작 위치. 알 수 없으면 null", example = "0")
            Integer startOffset,
            @Schema(description = "원문 종료 위치. 알 수 없으면 null", example = "920")
            Integer endOffset,
            @Schema(description = "추정 토큰 수. 알 수 없으면 null", example = "310")
            Integer tokenCount,
            @Schema(description = "청크 본문. 빈 값 불가, 1000자 이하", example = "스마트시티 통합 관제 플랫폼 구축을 위해 실시간 데이터 수집과 분석 기능이 필요하다.", requiredMode = Schema.RequiredMode.REQUIRED)
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

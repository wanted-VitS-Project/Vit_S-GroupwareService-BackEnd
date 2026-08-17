package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.mapper;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand.ChunkCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.ChunkEmbedding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DocumentChunkBatchMapper {

    // 한 파일의 청크 전체를 한 번의 INSERT ... ON DUPLICATE KEY UPDATE로 저장합니다.
    int upsertChunks(
            @Param("fileVersionId") Long fileVersionId,
            @Param("chunks") List<ChunkCommand> chunks,
            @Param("now") LocalDateTime now
    );

    // 임베딩이 끝난 청크들의 chroma_id를 한 번의 CASE 기반 UPDATE로 반영합니다.
    int updateChunkEmbeddings(
            @Param("fileVersionId") Long fileVersionId,
            @Param("chunks") List<ChunkEmbedding> chunks,
            @Param("embeddingModel") String embeddingModel,
            @Param("now") LocalDateTime now
    );
}

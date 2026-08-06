package com.group3.vitamins.vitamate.fileindex.application.port;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;

import java.util.List;
import java.util.Optional;

// 파일 인덱싱에 필요한 파일 정보 조회와 document_chunk 저장을 담당하는 포트
public interface VitamateFileIndexDataPort {

    Optional<VitamateFileIndexSourceResult> findIndexSource(Long fileVersionId);

    int replaceChunks(Long fileVersionId, List<SaveVitamateDocumentChunksCommand.ChunkCommand> chunks);
}
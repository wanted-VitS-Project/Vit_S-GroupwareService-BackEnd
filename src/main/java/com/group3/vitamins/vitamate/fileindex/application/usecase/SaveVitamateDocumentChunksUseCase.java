package com.group3.vitamins.vitamate.fileindex.application.usecase;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;

// 추출된 문서 청크를 저장하는 유스케이스
public interface SaveVitamateDocumentChunksUseCase {

    SaveVitamateDocumentChunksResult handle(SaveVitamateDocumentChunksCommand command);
}
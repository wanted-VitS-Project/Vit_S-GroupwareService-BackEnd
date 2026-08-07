package com.group3.vitamins.vitamate.fileindex.application.usecase;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateChunkEmbeddingsCommand;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateChunkEmbeddingsResult;

// Python worker가 만든 임베딩 저장 결과를 Spring DB에 반영하는 usecase입니다.
public interface SaveVitamateChunkEmbeddingsUseCase {

    SaveVitamateChunkEmbeddingsResult handle(SaveVitamateChunkEmbeddingsCommand command);
}

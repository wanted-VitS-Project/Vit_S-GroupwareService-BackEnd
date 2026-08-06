package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateChunkEmbeddingsCommand;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateChunkEmbeddingsCommand.ChunkEmbeddingCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.ChunkEmbedding;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateChunkEmbeddingsResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.SaveVitamateChunkEmbeddingsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Python worker가 ChromaDB에 저장한 임베딩 결과를 document_chunk에 반영합니다.
@Service
@RequiredArgsConstructor
@Transactional
public class VitamateChunkEmbeddingSaveService implements SaveVitamateChunkEmbeddingsUseCase {

    private static final int MAX_CHUNK_COUNT = 500;
    private static final int MAX_INDEX_ATTEMPT_ID_LENGTH = 36;
    private static final int MAX_EMBEDDING_MODEL_LENGTH = 100;
    private static final int MAX_CHROMA_ID_LENGTH = 150;
    private static final String COMPLETED_EMBEDDING_STATUS = "COMPLETED";

    private final VitamateFileIndexDataPort fileIndexDataPort;

    @Override
    public SaveVitamateChunkEmbeddingsResult handle(SaveVitamateChunkEmbeddingsCommand command) {
        validateCommand(command);

        if (!fileIndexDataPort.existsIndexableFileVersionForUpdate(command.fileVersionId())) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND);
        }

        int updatedChunkCount = fileIndexDataPort.updateChunkEmbeddings(
                command.fileVersionId(),
                command.indexAttemptId(),
                command.embeddingModel(),
                command.chunks().stream()
                        .map(chunk -> new ChunkEmbedding(
                                chunk.documentChunkId(),
                                chunk.chromaId()
                        ))
                        .toList()
        );

        if (updatedChunkCount != command.chunks().size()) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND);
        }

        return new SaveVitamateChunkEmbeddingsResult(
                command.fileVersionId(),
                command.indexAttemptId(),
                updatedChunkCount,
                COMPLETED_EMBEDDING_STATUS
        );
    }

    // 파일 버전, 모델명, chunk 목록의 기본 형식과 중복을 검증합니다.
    private void validateCommand(SaveVitamateChunkEmbeddingsCommand command) {
        if (command == null
                || command.fileVersionId() == null
                || command.fileVersionId() <= 0
                || command.indexAttemptId() == null
                || command.indexAttemptId().isBlank()
                || command.indexAttemptId().length() > MAX_INDEX_ATTEMPT_ID_LENGTH
                || command.embeddingModel() == null
                || command.embeddingModel().isBlank()
                || command.embeddingModel().length() > MAX_EMBEDDING_MODEL_LENGTH
                || command.chunks() == null
                || command.chunks().isEmpty()
                || command.chunks().size() > MAX_CHUNK_COUNT) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        validateChunks(command.chunks());
    }

    // 같은 요청 안에서 documentChunkId와 chromaId가 중복되지 않도록 막습니다.
    private void validateChunks(List<ChunkEmbeddingCommand> chunks) {
        Set<Long> documentChunkIds = new HashSet<>();
        Set<String> chromaIds = new HashSet<>();

        for (ChunkEmbeddingCommand chunk : chunks) {
            validateChunk(chunk);

            if (!documentChunkIds.add(chunk.documentChunkId())
                    || !chromaIds.add(chunk.chromaId())) {
                throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
            }
        }
    }

    // ChromaDB와 Spring DB를 연결하는 chunk 임베딩 결과 한 건을 검증합니다.
    private void validateChunk(ChunkEmbeddingCommand chunk) {
        if (chunk == null
                || chunk.documentChunkId() == null
                || chunk.documentChunkId() <= 0
                || chunk.chromaId() == null
                || chunk.chromaId().isBlank()
                || chunk.chromaId().length() > MAX_CHROMA_ID_LENGTH) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}

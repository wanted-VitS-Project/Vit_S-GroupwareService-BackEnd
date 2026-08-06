package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand.ChunkCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort.SavedDocumentChunk;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult.SavedChunkResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.SaveVitamateDocumentChunksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Python worker가 추출한 문서 청크를 기존 청크와 교체 저장합니다.
@Service
@RequiredArgsConstructor
@Transactional
public class VitamateDocumentChunkSaveService implements SaveVitamateDocumentChunksUseCase {

    private static final int MAX_EXCERPT_LENGTH = 1000;
    private static final int MAX_SECTION_TITLE_LENGTH = 255;
    private static final int MAX_CHUNK_COUNT = 500;

    private final VitamateFileIndexDataPort fileIndexDataPort;

    @Override
    public SaveVitamateDocumentChunksResult handle(SaveVitamateDocumentChunksCommand command) {
        validateCommand(command);

        if (!fileIndexDataPort.existsIndexableFileVersionForUpdate(command.fileVersionId())) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND);
        }

        List<SavedDocumentChunk> savedChunks = fileIndexDataPort.replaceChunks(command.fileVersionId(), command.chunks());

        return new SaveVitamateDocumentChunksResult(
                command.fileVersionId(),
                savedChunks.size(),
                savedChunks.stream()
                        .map(chunk -> new SavedChunkResult(
                                chunk.documentChunkId(),
                                chunk.chunkIndex(),
                                chunk.embeddingStatus()
                        ))
                        .toList()
        );
    }

    // 파일 버전 ID와 청크 목록 전체의 기본 형식을 검증합니다.
    private void validateCommand(SaveVitamateDocumentChunksCommand command) {
        if (command == null
                || command.fileVersionId() == null
                || command.fileVersionId() <= 0
                || command.chunks() == null
                || command.chunks().isEmpty()
                || command.chunks().size() > MAX_CHUNK_COUNT) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        validateChunks(command.chunks());
    }

    // 청크 순서 중복과 각 청크의 세부 값을 검증합니다.
    private void validateChunks(List<ChunkCommand> chunks) {
        Set<Integer> chunkIndexes = new HashSet<>();

        for (ChunkCommand chunk : chunks) {
            validateChunk(chunk);

            if (!chunkIndexes.add(chunk.chunkIndex())) {
                throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
            }
        }
    }

    // document_chunk 한 행으로 저장 가능한 값인지 검증합니다.
    private void validateChunk(ChunkCommand chunk) {
        if (chunk == null
                || chunk.chunkIndex() == null
                || chunk.chunkIndex() < 0
                || chunk.excerpt() == null
                || chunk.excerpt().isBlank()
                || chunk.excerpt().length() > MAX_EXCERPT_LENGTH) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (chunk.pageNumber() != null && chunk.pageNumber() <= 0) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (chunk.sectionTitle() != null && chunk.sectionTitle().length() > MAX_SECTION_TITLE_LENGTH) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (chunk.startOffset() != null && chunk.startOffset() < 0) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (chunk.endOffset() != null && chunk.endOffset() < 0) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (chunk.startOffset() != null
                && chunk.endOffset() != null
                && chunk.startOffset() > chunk.endOffset()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (chunk.tokenCount() != null && chunk.tokenCount() < 0) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}

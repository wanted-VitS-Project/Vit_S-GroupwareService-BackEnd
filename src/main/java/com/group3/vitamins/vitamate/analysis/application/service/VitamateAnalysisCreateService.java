package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.command.CreateVitamateAnalysisCommand;
import com.group3.vitamins.vitamate.analysis.application.command.DispatchVitamateAnalysisJobCommand;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort.ExistingAnalysis;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateBlockReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateFileReaderPort;
import com.group3.vitamins.vitamate.analysis.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.analysis.application.support.VitamateRequestHashGenerator;
import com.group3.vitamins.vitamate.analysis.application.usecase.CreateVitamateAnalysisUseCase;
import com.group3.vitamins.vitamate.analysis.application.usecase.DispatchVitamateAnalysisJobUseCase;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

// Creates Vitamate analysis requests and schedules worker dispatch after commit.
@Service
@RequiredArgsConstructor
public class VitamateAnalysisCreateService implements CreateVitamateAnalysisUseCase {

    private final VitamateAnalysisStorePort analysisStore;
    private final VitamateBlockReaderPort blockReader;
    private final VitamateFileReaderPort fileReader;
    private final VitamateRequestHashGenerator requestHashGenerator;
    private final DispatchVitamateAnalysisJobUseCase dispatchUseCase;

    // Coordinates validation, idempotency, persistence, and queue dispatch.
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public CreateVitamateAnalysisResult handle(CreateVitamateAnalysisCommand command) {
        validateCommand(command);

        VitamateBlockReaderPort.VitamateBlockContext blockContext = blockReader.findAccessibleVitamateBlock(
                command.blockId(),
                command.requestedBy()
        ).orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_BLOCK_NOT_FOUND));

        validateFileVersions(blockContext.projectId(), command.fileVersionIds());

        String requestHash = requestHashGenerator.generate(
                command.blockId(),
                command.fileVersionIds(),
                command.prompt()
        );

        Optional<ExistingAnalysis> existingAnalysis = analysisStore.findExistingAnalysis(
                blockContext.vitamateBlockId(),
                command.requestedBy(),
                command.idempotencyKey()
        );

        if (existingAnalysis.isPresent()) {
            return toCreateResultOrThrowConflict(existingAnalysis.get(), requestHash);
        }

        CreateVitamateAnalysisResult result;

        try {
            result = analysisStore.savePendingAnalysis(
                    new VitamateAnalysisStorePort.NewAnalysis(
                            blockContext.vitamateBlockId(),
                            command.requestedBy(),
                            command.idempotencyKey(),
                            requestHash,
                            command.prompt().trim(),
                            LocalDateTime.now()
                    )
            );
        } catch (DataIntegrityViolationException e) {
            return findExistingResultOrThrowConflict(
                    blockContext.vitamateBlockId(),
                    command.requestedBy(),
                    command.idempotencyKey(),
                    requestHash
            );
        }

        analysisStore.saveAnalysisDocuments(result.analysisId(), command.fileVersionIds());

        dispatchAfterCommit(result.analysisId());

        return result;
    }

    // Publishes the worker job after the request transaction is committed.
    private void dispatchAfterCommit(Long analysisId) {
        DispatchVitamateAnalysisJobCommand dispatchCommand = new DispatchVitamateAnalysisJobCommand(analysisId);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatchUseCase.handle(dispatchCommand);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatchUseCase.handle(dispatchCommand);
            }
        });
    }

    // Reuses an existing idempotent request only when the request body hash matches.
    private CreateVitamateAnalysisResult toCreateResultOrThrowConflict(
            ExistingAnalysis existingAnalysis,
            String requestHash
    ) {
        if (!existingAnalysis.requestHash().equals(requestHash)) {
            throw new ConflictException(VitamateErrorCode.VITAMATE_IDEMPOTENCY_CONFLICT);
        }

        return new CreateVitamateAnalysisResult(
                existingAnalysis.analysisId(),
                existingAnalysis.analysisStatus(),
                existingAnalysis.requestedAt()
        );
    }

    // Handles a concurrent unique-key race by reloading the existing request.
    private CreateVitamateAnalysisResult findExistingResultOrThrowConflict(
            Long vitamateBlockId,
            String requestedBy,
            String idempotencyKey,
            String requestHash
    ) {
        ExistingAnalysis existingAnalysis = analysisStore.findExistingAnalysis(
                vitamateBlockId,
                requestedBy,
                idempotencyKey
        ).orElseThrow(() -> new ConflictException(VitamateErrorCode.VITAMATE_IDEMPOTENCY_CONFLICT));

        return toCreateResultOrThrowConflict(existingAnalysis, requestHash);
    }

    // Validates required identifiers, prompt, file list, null elements, and duplicates.
    private void validateCommand(CreateVitamateAnalysisCommand command) {
        if (command == null
                || command.blockId() == null
                || isBlank(command.requestedBy())
                || isBlank(command.idempotencyKey())
                || isBlank(command.prompt())
                || command.fileVersionIds() == null
                || command.fileVersionIds().isEmpty()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (command.fileVersionIds().stream().anyMatch(fileVersionId -> fileVersionId == null)) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        int distinctCount = new HashSet<>(command.fileVersionIds()).size();
        if (distinctCount != command.fileVersionIds().size()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // Ensures every selected file version belongs to the same project and is completed.
    private void validateFileVersions(Long projectId, List<Long> fileVersionIds) {
        if (!fileReader.existsAllCompletedFileVersionsInProject(projectId, fileVersionIds)) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_INVALID);
        }
    }

    // Checks blank strings for command validation.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

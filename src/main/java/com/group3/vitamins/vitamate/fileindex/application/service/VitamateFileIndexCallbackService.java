package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.HandleVitamateFileIndexCallbackCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexCallbackResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.HandleVitamateFileIndexCallbackUseCase;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Validates and stores file indexing status callbacks from the Python worker.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VitamateFileIndexCallbackService implements HandleVitamateFileIndexCallbackUseCase {

    private final VitamateFileIndexStorePort fileIndexStore;

    @Override
    public VitamateFileIndexCallbackResult handle(HandleVitamateFileIndexCallbackCommand command) {
        validateCommand(command);

        FileIndexStatus status = parseStatus(command.indexStatus());
        validateStatusRule(status, command.errorMessage());

        if (!fileIndexStore.existsFileVersion(command.fileVersionId())) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND);
        }

        FileIndexStatus savedStatus = fileIndexStore.upsertStatus(
                command.fileVersionId(),
                status,
                command.errorMessage(),
                LocalDateTime.now()
        );

        log.info("Vitamate file index status saved - fileVersionId={}, indexStatus={}",
                command.fileVersionId(), savedStatus);

        return new VitamateFileIndexCallbackResult(
                true,
                command.fileVersionId(),
                savedStatus.name(),
                null
        );
    }

    // Checks required callback fields before touching the store.
    private void validateCommand(HandleVitamateFileIndexCallbackCommand command) {
        if (command == null
                || command.fileVersionId() == null
                || command.fileVersionId() <= 0
                || command.indexStatus() == null
                || command.indexStatus().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // Converts the raw callback status into the domain enum.
    private FileIndexStatus parseStatus(String rawStatus) {
        try {
            return FileIndexStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // Enforces state-specific errorMessage rules.
    private void validateStatusRule(FileIndexStatus status, String errorMessage) {
        if (status == FileIndexStatus.FAILED
                && (errorMessage == null || errorMessage.isBlank())) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (status != FileIndexStatus.FAILED
                && errorMessage != null
                && !errorMessage.isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}

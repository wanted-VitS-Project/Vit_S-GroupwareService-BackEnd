package com.group3.vitamins.vitamate.filecleanup.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.filecleanup.application.command.CleanupVitamateFileDerivedDataCommand;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateFileDerivedDataCleanupPort;
import com.group3.vitamins.vitamate.filecleanup.application.result.CleanupVitamateFileDerivedDataResult;
import com.group3.vitamins.vitamate.filecleanup.application.usecase.CleanupVitamateFileDerivedDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Removes Vitamate-derived data that belongs to a permanently deleted file.
@Service
@RequiredArgsConstructor
@Transactional
public class VitamateFileDerivedDataCleanupService implements CleanupVitamateFileDerivedDataUseCase {

    private final VitamateFileDerivedDataCleanupPort cleanupPort;

    @Override
    public CleanupVitamateFileDerivedDataResult handle(CleanupVitamateFileDerivedDataCommand command) {
        validate(command);
        return cleanupPort.cleanupByFileId(command.fileId());
    }

    // Ensures cleanup is executed with a concrete positive file id.
    private void validate(CleanupVitamateFileDerivedDataCommand command) {
        if (command == null || command.fileId() == null || command.fileId() <= 0) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}

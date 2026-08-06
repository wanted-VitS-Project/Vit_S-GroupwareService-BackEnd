package com.group3.vitamins.vitamate.filecleanup.infrastructure.adapter;

import com.group3.vitamins.file.application.port.FileDerivedDataCleanupPort;
import com.group3.vitamins.vitamate.filecleanup.application.command.CleanupVitamateFileDerivedDataCommand;
import com.group3.vitamins.vitamate.filecleanup.application.usecase.CleanupVitamateFileDerivedDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Adapts file-domain cleanup requests to Vitamate derived-data cleanup.
@Component
@RequiredArgsConstructor
public class VitamateFileDerivedDataCleanupAdapter implements FileDerivedDataCleanupPort {

    private final CleanupVitamateFileDerivedDataUseCase cleanupUseCase;

    @Override
    public void cleanupByFileId(Long fileId) {
        cleanupUseCase.handle(new CleanupVitamateFileDerivedDataCommand(fileId));
    }
}

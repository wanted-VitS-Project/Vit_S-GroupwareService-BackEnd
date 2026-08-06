package com.group3.vitamins.vitamate.filecleanup.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.filecleanup.application.command.CleanupVitamateFileDerivedDataCommand;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateFileDerivedDataCleanupPort;
import com.group3.vitamins.vitamate.filecleanup.application.result.CleanupVitamateFileDerivedDataResult;
import com.group3.vitamins.vitamate.filecleanup.application.usecase.CleanupVitamateFileDerivedDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 파일 삭제에 따라 비타메이트 파생 데이터를 정리하는 서비스
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

    // fileId 없이 정리하면 다른 파일 데이터까지 건드릴 수 있어 방어한다.
    private void validate(CleanupVitamateFileDerivedDataCommand command) {
        if (command == null || command.fileId() == null || command.fileId() <= 0) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}
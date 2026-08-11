package com.group3.vitamins.vitamate.filecleanup.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.filecleanup.application.command.CleanupVitamateFileDerivedDataCommand;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupJobStorePort;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateFileDerivedDataCleanupPort;
import com.group3.vitamins.vitamate.filecleanup.application.result.CleanupVitamateFileDerivedDataResult;
import com.group3.vitamins.vitamate.filecleanup.application.usecase.CleanupVitamateFileDerivedDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 영구 삭제되는 파일에 속한 비타메이트 파생 데이터를 정리합니다.
@Service
@RequiredArgsConstructor
@Transactional
public class VitamateFileDerivedDataCleanupService implements CleanupVitamateFileDerivedDataUseCase {

    private final VitamateFileDerivedDataCleanupPort cleanupPort;
    private final VitamateCleanupJobStorePort cleanupJobStorePort;

    @Override
    public CleanupVitamateFileDerivedDataResult handle(
            CleanupVitamateFileDerivedDataCommand command
    ) {
        // 잘못된 값이 저장 포트까지 전달되지 않게 합니다.
        validate(command);

        // 파일 버전이 삭제되기 전에 정리 작업을 등록합니다.
        cleanupJobStorePort.createCleanupJob(command.fileId());

        return cleanupPort.cleanupByFileId(command.fileId());
    }

    // 정리 작업이 유효한 파일 ID로만 실행되도록 검증합니다.
    private void validate(CleanupVitamateFileDerivedDataCommand command) {
        if (command == null || command.fileId() == null || command.fileId() <= 0) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}

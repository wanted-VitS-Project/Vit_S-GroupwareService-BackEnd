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

// Python worker의 파일 인덱싱 상태 callback을 검증하고 저장한다.
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

        log.info("비타메이트 파일 인덱싱 상태 저장 - fileVersionId={}, indexStatus={}",
                command.fileVersionId(), savedStatus);

        return new VitamateFileIndexCallbackResult(
                true,
                command.fileVersionId(),
                savedStatus.name(),
                null
        );
    }

    // callback command의 필수값을 검증한다.
    private void validateCommand(HandleVitamateFileIndexCallbackCommand command) {
        if (command == null
                || command.fileVersionId() == null
                || command.fileVersionId() <= 0
                || command.indexStatus() == null
                || command.indexStatus().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // 문자열 상태값을 enum으로 변환한다.
    private FileIndexStatus parseStatus(String rawStatus) {
        try {
            return FileIndexStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // 상태별 errorMessage 규칙을 검증한다.
    private void validateStatusRule(FileIndexStatus status, String errorMessage) {
        if (status == FileIndexStatus.PENDING) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (status == FileIndexStatus.FAILED
                && (errorMessage == null || errorMessage.isBlank())) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if ((status == FileIndexStatus.PROCESSING || status == FileIndexStatus.COMPLETED)
                && errorMessage != null
                && !errorMessage.isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}
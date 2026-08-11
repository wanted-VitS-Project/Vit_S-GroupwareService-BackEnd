package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.HandleVitamateFileIndexCallbackCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort.FileIndexStatusUpdateResult;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexCallbackResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.HandleVitamateFileIndexCallbackUseCase;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Python worker가 전달한 파일 인덱싱 상태 callback을 검증하고 저장합니다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VitamateFileIndexCallbackService implements HandleVitamateFileIndexCallbackUseCase {

    private static final int MAX_INDEX_ATTEMPT_ID_LENGTH = 36;

    private final VitamateFileIndexStorePort fileIndexStore;

    @Override
    public VitamateFileIndexCallbackResult handle(HandleVitamateFileIndexCallbackCommand command) {
        validateCommand(command);

        FileIndexStatus status = parseStatus(command.indexStatus());
        validateStatusRule(status, command.indexAttemptId(), command.errorMessage());

        if (!fileIndexStore.existsFileVersion(command.fileVersionId())) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND);
        }

        FileIndexStatusUpdateResult statusUpdateResult = fileIndexStore.upsertStatus(
                command.fileVersionId(),
                command.indexAttemptId(),
                status,
                command.errorMessage(),
                LocalDateTime.now()
        );

        if (!statusUpdateResult.accepted()) {
            log.warn("Vitamate file index callback ignored - fileVersionId={}, indexAttemptId={}, indexStatus={}, reason={}",
                    command.fileVersionId(), command.indexAttemptId(), status, statusUpdateResult.reason());

            return new VitamateFileIndexCallbackResult(
                    false,
                    command.fileVersionId(),
                    statusUpdateResult.indexAttemptId(),
                    status.name(),
                    statusUpdateResult.reason()
            );
        }

        log.info("Vitamate file index status saved - fileVersionId={}, indexAttemptId={}, indexStatus={}",
                command.fileVersionId(), statusUpdateResult.indexAttemptId(), statusUpdateResult.indexStatus());

        return new VitamateFileIndexCallbackResult(
                true,
                command.fileVersionId(),
                statusUpdateResult.indexAttemptId(),
                statusUpdateResult.indexStatus().name(),
                null
        );
    }

    // 저장소에 접근하기 전에 callback 필수 값을 검증합니다.
    private void validateCommand(HandleVitamateFileIndexCallbackCommand command) {
        if (command == null
                || command.fileVersionId() == null
                || command.fileVersionId() <= 0
                || command.indexStatus() == null
                || command.indexStatus().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // 문자열 상태값을 도메인 enum으로 변환합니다.
    private FileIndexStatus parseStatus(String rawStatus) {
        try {
            return FileIndexStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // 상태별 indexAttemptId와 errorMessage 규칙을 검증합니다.
    private void validateStatusRule(FileIndexStatus status, String indexAttemptId, String errorMessage) {
        if ((status == FileIndexStatus.COMPLETED || status == FileIndexStatus.FAILED)
                && (indexAttemptId == null || indexAttemptId.isBlank())) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (indexAttemptId != null && indexAttemptId.length() > MAX_INDEX_ATTEMPT_ID_LENGTH) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

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

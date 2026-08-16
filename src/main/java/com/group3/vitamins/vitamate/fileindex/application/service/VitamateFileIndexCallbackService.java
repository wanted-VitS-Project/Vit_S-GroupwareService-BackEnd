package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.command.HandleVitamateFileIndexCallbackCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexJobPublisherPort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort.FileIndexStatusUpdateResult;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexCallbackResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.HandleVitamateFileIndexCallbackUseCase;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

// Python worker가 전달한 파일 인덱싱 상태 callback을 검증하고 저장합니다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VitamateFileIndexCallbackService implements HandleVitamateFileIndexCallbackUseCase {

    private static final int MAX_INDEX_ATTEMPT_ID_LENGTH = 36;

    private final VitamateFileIndexStorePort fileIndexStore;
    private final VitamateFileIndexJobPublisherPort jobPublisherPort;

    @Override
    public VitamateFileIndexCallbackResult handle(HandleVitamateFileIndexCallbackCommand command) {
        validateCommand(command);

        FileIndexStatus status = parseStatus(command.indexStatus());
        validateStatusRule(status, command.indexAttemptId(), command.errorMessage());

        if (!fileIndexStore.existsFileVersion(command.fileVersionId())) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        FileIndexStatusUpdateResult statusUpdateResult = fileIndexStore.upsertStatus(
                command.fileVersionId(),
                command.indexAttemptId(),
                status,
                command.errorMessage(),
                command.retryable(),
                now
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

        if (statusUpdateResult.requeued()) {
            log.warn("Vitamate file index retryable failure requeued - fileVersionId={}, oldIndexAttemptId={}, newIndexAttemptId={}",
                    command.fileVersionId(), command.indexAttemptId(), statusUpdateResult.indexAttemptId());
            registerAfterCommitRequeue(command.fileVersionId(), now);
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

    // 이 콜백 트랜잭션이 실제로 커밋된 뒤에만 재발행한다 — 커밋 전에 발행하면, 이후 같은
    // 트랜잭션에서 다른 이유로 롤백될 때 DB는 되돌아가는데 큐에는 이미 작업이 나간 상태가 된다.
    private void registerAfterCommitRequeue(Long fileVersionId, LocalDateTime now) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishRequeue(fileVersionId, now);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishRequeue(fileVersionId, now);
            }
        });
    }

    // 재발행에 실패해도 즉시 재시도 상한을 다시 채우지는 않는다 — PENDING으로 남아 있으므로
    // 재시도 스케줄러의 lease 만료 재claim이 안전망으로 나중에 다시 집어간다.
    private void publishRequeue(Long fileVersionId, LocalDateTime now) {
        try {
            jobPublisherPort.publish(new VitamateFileIndexJobPublisherPort.FileIndexJob(fileVersionId, 0, now));
        } catch (RuntimeException e) {
            log.error("Vitamate file index requeue publish failed - fileVersionId={}", fileVersionId, e);
        }
    }
}

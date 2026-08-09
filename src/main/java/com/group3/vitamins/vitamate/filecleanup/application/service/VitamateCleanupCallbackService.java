package com.group3.vitamins.vitamate.filecleanup.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.filecleanup.application.command.HandleVitamateCleanupCallbackCommand;
import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupJob;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupJobStorePort;
import com.group3.vitamins.vitamate.filecleanup.application.result.VitamateCleanupCallbackResult;
import com.group3.vitamins.vitamate.filecleanup.application.usecase.HandleVitamateCleanupCallbackUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

// Python worker가 전달한 ChromaDB 정리 결과를 현재 시도와 대조해 반영합니다.
@Service
@RequiredArgsConstructor
public class VitamateCleanupCallbackService implements HandleVitamateCleanupCallbackUseCase {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_ATTEMPT_ID_LENGTH = 36;
    private static final int MAX_ERROR_CODE_LENGTH = 100;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final String IGNORED_REASON = "attempt_mismatch_or_already_finished";

    private final VitamateCleanupJobStorePort cleanupJobStorePort;
    private final Clock clock;

    @Override
    @Transactional
    public VitamateCleanupCallbackResult handle(HandleVitamateCleanupCallbackCommand command) {
        CallbackStatus callbackStatus = validateCommand(command);
        int attemptCount = cleanupJobStorePort.findAttemptCount(command.cleanupJobId())
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_CLEANUP_JOB_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);

        return switch (callbackStatus) {
            case PROCESSING -> result(command.cleanupJobId(), cleanupJobStorePort.markProcessing(
                    command.cleanupJobId(), command.attemptId(), now
            ), VitamateCleanupJob.Status.PROCESSING);
            case COMPLETED -> result(command.cleanupJobId(), cleanupJobStorePort.markCompleted(
                    command.cleanupJobId(), command.attemptId(), command.deletedVectorCount(), now
            ), VitamateCleanupJob.Status.COMPLETED);
            case FAILED -> handleFailed(command, attemptCount, now);
        };
    }

    // 재시도 가능한 실패는 다음 Outbox를 예약하고, 한도 초과나 영구 실패는 DLQ 상태로 마감합니다.
    private VitamateCleanupCallbackResult handleFailed(
            HandleVitamateCleanupCallbackCommand command,
            int attemptCount,
            LocalDateTime now
    ) {
        if (Boolean.TRUE.equals(command.retryable()) && attemptCount < MAX_ATTEMPTS) {
            boolean scheduled = cleanupJobStorePort.scheduleRetry(
                    command.cleanupJobId(),
                    command.attemptId(),
                    command.errorCode(),
                    command.errorMessage(),
                    MAX_ATTEMPTS,
                    now.plusSeconds(retryDelaySeconds(attemptCount)),
                    now
            );
            if (scheduled) {
                return VitamateCleanupCallbackResult.accepted(
                        command.cleanupJobId(), VitamateCleanupJob.Status.RETRY_WAIT.name()
                );
            }
        }

        boolean deadLettered = cleanupJobStorePort.markDeadLetter(
                command.cleanupJobId(),
                command.attemptId(),
                command.errorCode(),
                command.errorMessage(),
                now
        );
        return result(command.cleanupJobId(), deadLettered, VitamateCleanupJob.Status.DEAD_LETTER);
    }

    // 최초 실행을 포함한 현재 처리 횟수에 맞춰 10초, 30초, 1분, 5분 간격을 적용합니다.
    private long retryDelaySeconds(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 10L;
            case 2 -> 30L;
            case 3 -> 60L;
            default -> 300L;
        };
    }

    // 상태별 필수값과 null 규칙을 검증하고 callback 상태를 변환합니다.
    private CallbackStatus validateCommand(HandleVitamateCleanupCallbackCommand command) {
        if (command == null
                || command.cleanupJobId() == null
                || command.cleanupJobId() <= 0
                || isBlank(command.attemptId())
                || command.attemptId().length() > MAX_ATTEMPT_ID_LENGTH
                || isBlank(command.status())
                || command.retryable() == null) {
            throw invalidRequest();
        }

        CallbackStatus status;
        try {
            status = CallbackStatus.valueOf(command.status());
        } catch (IllegalArgumentException exception) {
            throw invalidRequest();
        }

        if (status == CallbackStatus.COMPLETED) {
            validateCompleted(command);
        } else if (status == CallbackStatus.FAILED) {
            validateFailed(command);
        } else if (Boolean.TRUE.equals(command.retryable())
                || command.deletedVectorCount() != null
                || !isBlank(command.errorCode())
                || !isBlank(command.errorMessage())) {
            throw invalidRequest();
        }
        return status;
    }

    private void validateCompleted(HandleVitamateCleanupCallbackCommand command) {
        if (Boolean.TRUE.equals(command.retryable())
                || command.deletedVectorCount() == null
                || command.deletedVectorCount() < 0
                || !isBlank(command.errorCode())
                || !isBlank(command.errorMessage())) {
            throw invalidRequest();
        }
    }

    private void validateFailed(HandleVitamateCleanupCallbackCommand command) {
        if (command.deletedVectorCount() != null
                || isBlank(command.errorCode())
                || command.errorCode().length() > MAX_ERROR_CODE_LENGTH
                || isBlank(command.errorMessage())
                || command.errorMessage().length() > MAX_ERROR_MESSAGE_LENGTH) {
            throw invalidRequest();
        }
    }

    private VitamateCleanupCallbackResult result(
            Long cleanupJobId,
            boolean accepted,
            VitamateCleanupJob.Status acceptedStatus
    ) {
        if (accepted) {
            return VitamateCleanupCallbackResult.accepted(cleanupJobId, acceptedStatus.name());
        }

        String currentStatus = cleanupJobStorePort.findStatus(cleanupJobId)
                .map(Enum::name)
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_CLEANUP_JOB_NOT_FOUND));
        return VitamateCleanupCallbackResult.rejected(cleanupJobId, currentStatus, IGNORED_REASON);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ValidationException invalidRequest() {
        return new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
    }

    private enum CallbackStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }
}

package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
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
import com.group3.vitamins.vitamate.analysis.domain.exception.VitamateErrorCode;
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

// 비타메이트 분석 요청 생성 흐름을 처리하는 서비스
@Service
@RequiredArgsConstructor
public class VitamateAnalysisCreateService implements CreateVitamateAnalysisUseCase {

    private final VitamateAnalysisStorePort analysisStore;
    private final VitamateBlockReaderPort blockReader;
    private final VitamateFileReaderPort fileReader;
    private final VitamateRequestHashGenerator requestHashGenerator;
    private final DispatchVitamateAnalysisJobUseCase dispatchUseCase;

    // 분석 요청 생성 전체 흐름을 조율한다.
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

        // DB 커밋이 끝난 뒤 Python worker가 조회할 큐 메시지를 발행한다.
        dispatchAfterCommit(result.analysisId());

        return result;
    }

    // 트랜잭션 커밋 이후에 비동기 작업 큐로 분석 요청을 전달한다.
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

    // 기존 멱등성 요청이 같은 본문인지 확인하고 응답 결과로 변환한다.
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

    // 동시 요청으로 unique 충돌이 난 경우 저장된 기존 요청을 다시 조회한다.
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

    // 서비스 진입 시점에 필수값, 빈 목록, null 원소, 중복 파일 버전 ID를 검증한다.
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

    // 선택한 파일 버전들이 모두 같은 프로젝트의 완료된 파일인지 확인한다.
    private void validateFileVersions(Long projectId, List<Long> fileVersionIds) {
        if (!fileReader.existsAllCompletedFileVersionsInProject(projectId, fileVersionIds)) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_INVALID);
        }
    }

    // 문자열 필수값 검증을 위한 공통 보조 메서드다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

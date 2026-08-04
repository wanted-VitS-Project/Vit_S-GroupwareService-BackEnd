package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.vitamate.application.command.CreateVitamateAnalysisCommand;
import com.group3.vitamins.vitamate.application.port.VitamateAnalysisStore;
import com.group3.vitamins.vitamate.application.port.VitamateBlockReader;
import com.group3.vitamins.vitamate.application.port.VitamateFileReader;
import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.application.support.VitamateRequestHashGenerator;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.application.port.VitamateAnalysisStore.ExistingAnalysis;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

// 비타메이트 분석 요청 생성 흐름을 처리하는 서비스
@Service
@RequiredArgsConstructor
public class VitamateAnalysisCreateService {

    private final VitamateAnalysisStore analysisStore;
    private final VitamateBlockReader blockReader;
    private final VitamateFileReader fileReader;
    private final VitamateRequestHashGenerator requestHashGenerator;

    @Transactional(propagation = Propagation.REQUIRED)
    public CreateVitamateAnalysisResult create(CreateVitamateAnalysisCommand command) {
        validateCommand(command);

        VitamateBlockReader.VitamateBlockContext blockContext = blockReader.findAccessibleVitamateBlock(
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
            if (!existingAnalysis.get().requestHash().equals(requestHash)) {
                throw new ConflictException(VitamateErrorCode.VITAMATE_IDEMPOTENCY_CONFLICT);
            }

            return new CreateVitamateAnalysisResult(
                    existingAnalysis.get().analysisId(),
                    existingAnalysis.get().analysisStatus(),
                    existingAnalysis.get().requestedAt()
            );
        }

        CreateVitamateAnalysisResult result = analysisStore.savePendingAnalysis(
                new VitamateAnalysisStore.NewAnalysis(
                        blockContext.vitamateBlockId(),
                        command.requestedBy(),
                        command.idempotencyKey(),
                        requestHash,
                        command.prompt().trim(),
                        LocalDateTime.now()
                )
        );

        analysisStore.saveAnalysisDocuments(result.analysisId(), command.fileVersionIds());

        return result;
    }
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

    private void validateFileVersions(Long projectId, List<Long> fileVersionIds) {
        if (!fileReader.existsAllCompletedFileVersionsInProject(projectId, fileVersionIds)) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_INVALID);
        }
    }
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

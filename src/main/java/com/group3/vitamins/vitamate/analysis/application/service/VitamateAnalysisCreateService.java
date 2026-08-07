package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.command.CreateVitamateAnalysisCommand;
import com.group3.vitamins.vitamate.analysis.application.command.DispatchVitamateAnalysisJobCommand;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort.ExistingAnalysis;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateBlockReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateFileReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateReviewTemplateReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateReviewTemplateReaderPort.ReviewTemplate;
import com.group3.vitamins.vitamate.analysis.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.analysis.application.support.VitamateRequestHashGenerator;
import com.group3.vitamins.vitamate.analysis.application.usecase.CreateVitamateAnalysisUseCase;
import com.group3.vitamins.vitamate.analysis.application.usecase.DispatchVitamateAnalysisJobUseCase;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// 비타메이트 분석 요청을 생성하고 커밋 이후 worker 작업을 예약합니다.
@Service
@RequiredArgsConstructor
public class VitamateAnalysisCreateService implements CreateVitamateAnalysisUseCase {

    private final VitamateAnalysisStorePort analysisStore;
    private final VitamateBlockReaderPort blockReader;
    private final VitamateFileReaderPort fileReader;
    private final VitamateReviewTemplateReaderPort templateReader;
    private final VitamateRequestHashGenerator requestHashGenerator;
    private final DispatchVitamateAnalysisJobUseCase dispatchUseCase;

    // 요청 검증, 멱등성 처리, 저장, 큐 발행 예약을 한 흐름으로 조율합니다.
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public CreateVitamateAnalysisResult handle(CreateVitamateAnalysisCommand command) {
        validateCommand(command);

        String reviewType = normalizeRequired(command.reviewType());
        List<String> reviewCategoryCodes = normalizeReviewCategoryCodes(command.reviewCategoryCodes());
        String additionalInstruction = normalizeOptional(command.additionalInstruction());
        List<ReviewTemplate> reviewTemplates = findSelectedTemplates(reviewType, reviewCategoryCodes);

        VitamateBlockReaderPort.VitamateBlockContext blockContext = blockReader.findAccessibleVitamateBlock(
                command.blockId(),
                command.requestedBy()
        ).orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_BLOCK_NOT_FOUND));

        validateFileVersions(blockContext.projectId(), command.fileVersionIds());

        String requestHash = requestHashGenerator.generate(
                command.blockId(),
                command.fileVersionIds(),
                reviewType,
                reviewCategoryCodes,
                additionalInstruction
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
                            createPromptSummary(reviewType, reviewCategoryCodes, additionalInstruction),
                            reviewType,
                            joinCategoryCodes(reviewCategoryCodes),
                            additionalInstruction,
                            createPromptTemplateVersion(reviewTemplates),
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
        analysisStore.saveAnalysisTemplates(result.analysisId(), toNewAnalysisTemplates(reviewTemplates));

        dispatchAfterCommit(result.analysisId());

        return result;
    }

    // 요청 트랜잭션이 커밋된 뒤 worker job을 발행합니다.
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

    // 같은 멱등성 키의 요청은 본문 해시가 같을 때만 기존 결과를 재사용합니다.
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

    // 동시에 같은 멱등성 키가 저장된 경우 기존 요청을 다시 읽어 충돌 여부를 판단합니다.
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

    // 필수 식별자, 파일 목록, 검토 유형, 검토 카테고리 목록을 1차 검증합니다.
    private void validateCommand(CreateVitamateAnalysisCommand command) {
        if (command == null
                || command.blockId() == null
                || isBlank(command.requestedBy())
                || isBlank(command.idempotencyKey())
                || isBlank(command.reviewType())
                || command.fileVersionIds() == null
                || command.fileVersionIds().isEmpty()
                || command.reviewCategoryCodes() == null
                || command.reviewCategoryCodes().isEmpty()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (command.fileVersionIds().stream().anyMatch(fileVersionId -> fileVersionId == null)) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (command.reviewCategoryCodes().stream().anyMatch(this::isBlank)) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        int distinctFileCount = new HashSet<>(command.fileVersionIds()).size();
        if (distinctFileCount != command.fileVersionIds().size()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // 카테고리 코드의 blank와 중복을 제거하지 않고 오류로 잡아낸 뒤 표준 문자열로 변환합니다.
    private List<String> normalizeReviewCategoryCodes(List<String> rawCategoryCodes) {
        Set<String> uniqueCategoryCodes = new LinkedHashSet<>();
        List<String> categoryCodes = new ArrayList<>();

        for (String rawCategoryCode : rawCategoryCodes) {
            String categoryCode = normalizeRequired(rawCategoryCode);
            if (!uniqueCategoryCodes.add(categoryCode)) {
                throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
            }
            categoryCodes.add(categoryCode);
        }

        return categoryCodes;
    }

    // DB 마스터에 등록된 활성 템플릿과 요청 카테고리가 정확히 일치하는지 확인합니다.
    private List<ReviewTemplate> findSelectedTemplates(String reviewType, List<String> reviewCategoryCodes) {
        List<ReviewTemplate> templates = templateReader.findActiveTemplates(reviewType, reviewCategoryCodes);
        Set<String> foundCategoryCodes = templates.stream()
                .map(ReviewTemplate::categoryCode)
                .collect(Collectors.toSet());

        if (templates.size() != reviewCategoryCodes.size()
                || !foundCategoryCodes.containsAll(reviewCategoryCodes)) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        return templates;
    }

    // 분석 요청에 사용한 템플릿 버전이 여러 개면 혼합 버전으로 저장합니다.
    private String createPromptTemplateVersion(List<ReviewTemplate> reviewTemplates) {
        Set<String> versions = reviewTemplates.stream()
                .map(ReviewTemplate::templateVersion)
                .collect(Collectors.toSet());

        if (versions.size() == 1) {
            return versions.iterator().next();
        }

        return "MULTI_VERSION";
    }

    // 선택된 템플릿을 저장 포트의 스냅샷 입력값으로 변환합니다.
    private List<VitamateAnalysisStorePort.NewAnalysisTemplate> toNewAnalysisTemplates(List<ReviewTemplate> templates) {
        return templates.stream()
                .map(template -> new VitamateAnalysisStorePort.NewAnalysisTemplate(
                        template.reviewType(),
                        template.categoryCode(),
                        template.categoryName(),
                        template.promptTemplate(),
                        template.templateVersion(),
                        template.sortOrder()
                ))
                .toList();
    }

    // DB 저장용 카테고리 코드를 쉼표 구분 문자열로 변환합니다.
    private String joinCategoryCodes(List<String> reviewCategoryCodes) {
        return String.join(",", reviewCategoryCodes);
    }

    // 기존 prompt 컬럼에는 프롬프트 전문이 아니라 요청 요약만 남깁니다.
    private String createPromptSummary(
            String reviewType,
            List<String> reviewCategoryCodes,
            String additionalInstruction
    ) {
        String summary = "reviewType=" + reviewType
                + "; categories=" + joinCategoryCodes(reviewCategoryCodes);

        if (additionalInstruction == null) {
            return summary;
        }

        return summary + "; additionalInstruction=" + additionalInstruction;
    }

    // 필수 문자열 요청값을 trim한 표준값으로 변환합니다.
    private String normalizeRequired(String value) {
        if (isBlank(value)) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        return value.trim();
    }

    // 사용자가 추가 요청을 비워 보낸 경우 null로 통일합니다.
    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // 선택한 모든 파일 버전이 같은 프로젝트의 완료된 파일인지 확인합니다.
    private void validateFileVersions(Long projectId, List<Long> fileVersionIds) {
        if (!fileReader.existsAllCompletedFileVersionsInProject(projectId, fileVersionIds)) {
            throw new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_INVALID);
        }
    }

    // 문자열 필수값 검증에 사용하는 공통 blank 검사입니다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

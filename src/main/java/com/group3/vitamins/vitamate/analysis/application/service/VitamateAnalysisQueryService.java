package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateBlockReaderPort;
import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateAnalysisQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisDetailResult;
import com.group3.vitamins.vitamate.analysis.application.usecase.GetVitamateAnalysisUseCase;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Reads Vitamate analysis status, result, selected documents, and citations.
@Service
@RequiredArgsConstructor
public class VitamateAnalysisQueryService implements GetVitamateAnalysisUseCase {

    private final VitamateAnalysisReaderPort analysisReaderPort;
    private final VitamateBlockReaderPort blockReaderPort;
    private final StepAccessUseCase stepAccessUseCase;

    // Finds an analysis detail only when the requester can access its block.
    @Override
    @Transactional(readOnly = true)
    public VitamateAnalysisDetailResult handle(GetVitamateAnalysisQuery query) {
        validateQuery(query);

        VitamateAnalysisReaderPort.VitamateAnalysisDetail analysis = analysisReaderPort.findAnalysis(query.analysisId())
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_ANALYSIS_NOT_FOUND));
        stepAccessUseCase.requireAccess(resolveStepId(analysis.blockId()), query.userId(), query.role());
        return VitamateAnalysisDetailResult.from(analysis);
    }

    // Validates the identifiers required for analysis lookup.
    private void validateQuery(GetVitamateAnalysisQuery query) {
        if (query == null
                || query.analysisId() == null
                || query.analysisId() <= 0
                || query.userId() == null
                || query.userId().isBlank()
                || query.role() == null
                || query.role().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    private Long resolveStepId(Long blockId) {
        return blockReaderPort.findVitamateBlock(blockId)
                .map(VitamateBlockReaderPort.VitamateBlockContext::stepId)
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_ANALYSIS_NOT_FOUND));
    }
}

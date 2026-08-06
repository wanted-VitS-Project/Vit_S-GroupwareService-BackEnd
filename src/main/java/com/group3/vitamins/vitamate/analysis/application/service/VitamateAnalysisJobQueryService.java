package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateAnalysisJobQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisJobDetailResult;
import com.group3.vitamins.vitamate.analysis.application.usecase.GetVitamateAnalysisJobUseCase;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Loads the processing analysis job payload consumed by the Python worker.
@Service
@RequiredArgsConstructor
public class VitamateAnalysisJobQueryService implements GetVitamateAnalysisJobUseCase {

    private final VitamateAnalysisReaderPort analysisReaderPort;

    // Finds a PROCESSING job only when the attemptId and lease are still valid.
    @Override
    @Transactional(readOnly = true)
    public VitamateAnalysisJobDetailResult handle(GetVitamateAnalysisJobQuery query) {
        validateQuery(query);

        return analysisReaderPort.findProcessingAnalysisJob(query.analysisId(), query.attemptId())
                .map(VitamateAnalysisJobDetailResult::from)
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_ANALYSIS_NOT_FOUND));
    }

    // Validates the identifiers required for worker job lookup.
    private void validateQuery(GetVitamateAnalysisJobQuery query) {
        if (query == null
                || query.analysisId() == null
                || query.analysisId() <= 0
                || query.attemptId() == null
                || query.attemptId().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}

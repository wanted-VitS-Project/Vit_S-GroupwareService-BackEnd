package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateAnalysisJobQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisJobDetailResult;
import com.group3.vitamins.vitamate.analysis.application.usecase.GetVitamateAnalysisJobUseCase;
import com.group3.vitamins.vitamate.analysis.domain.exception.VitamateErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Python worker가 처리할 비타메이트 분석 작업 입력 조회 서비스
@Service
@RequiredArgsConstructor
public class VitamateAnalysisJobQueryService implements GetVitamateAnalysisJobUseCase {

    private final VitamateAnalysisReaderPort analysisReaderPort;

    // PROCESSING 상태와 attemptId가 일치하는 분석 작업 입력을 조회한다.
    @Override
    @Transactional(readOnly = true)
    public VitamateAnalysisJobDetailResult handle(GetVitamateAnalysisJobQuery query) {
        validateQuery(query);

        return analysisReaderPort.findProcessingAnalysisJob(query.analysisId(), query.attemptId())
                .map(VitamateAnalysisJobDetailResult::from)
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_ANALYSIS_NOT_FOUND));
    }

    // 내부 작업 조회에 필요한 분석 ID와 attemptId가 유효한지 확인한다.
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

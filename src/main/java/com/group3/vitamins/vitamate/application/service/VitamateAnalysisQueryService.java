package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.application.port.VitamateAnalysisReader;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisDetailResult;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비타메이트 분석 결과와 블록별 이력 조회를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class VitamateAnalysisQueryService {

    private final VitamateAnalysisReader analysisReader;

    // 요청자가 접근 가능한 분석 상세를 조회한다.
    @Transactional(readOnly = true)
    public VitamateAnalysisDetailResult getAnalysis(Long analysisId, String userId) {
        validateQuery(analysisId, userId);

        return analysisReader.findAccessibleAnalysis(analysisId, userId)
                .map(VitamateAnalysisDetailResult::from)
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_ANALYSIS_NOT_FOUND));
    }

    // 조회에 필요한 식별자가 비어 있지 않은지 확인한다.
    private void validateQuery(Long analysisId, String userId) {
        if (analysisId == null || analysisId <= 0 || userId == null || userId.isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}

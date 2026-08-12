package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateBlockReaderPort;
import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateBlockAnalysisHistoryQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisHistoryResult;
import com.group3.vitamins.vitamate.analysis.application.usecase.GetVitamateBlockAnalysisHistoryUseCase;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 비타메이트 블록 접근 권한을 확인한 뒤, 해당 블록의 분석 실행 이력을 조회하는 서비스입니다.
@Service
@RequiredArgsConstructor
public class VitamateAnalysisHistoryQueryService implements GetVitamateBlockAnalysisHistoryUseCase {

    private static final int HISTORY_LIMIT = 20;

    private final VitamateBlockReaderPort blockReader;
    private final VitamateAnalysisReaderPort analysisReader;
    private final StepAccessUseCase stepAccessUseCase;

    @Override
    @Transactional(readOnly = true)
    public VitamateAnalysisHistoryResult handle(GetVitamateBlockAnalysisHistoryQuery query) {
        validateQuery(query);

        VitamateBlockReaderPort.VitamateBlockContext blockContext = blockReader.findVitamateBlock(query.blockId())
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_BLOCK_NOT_FOUND));
        stepAccessUseCase.requireAccess(blockContext.stepId(), query.userId(), query.role());

        return VitamateAnalysisHistoryResult.from(
                blockContext.blockId(),
                analysisReader.findBlockAnalysisHistories(blockContext.vitamateBlockId(), HISTORY_LIMIT)
        );
    }

    // 분석 이력 조회에 필요한 blockId와 userId가 올바른지 검증합니다.
    private void validateQuery(GetVitamateBlockAnalysisHistoryQuery query) {
        if (query == null
                || query.blockId() == null
                || query.blockId() <= 0
                || query.userId() == null
                || query.userId().isBlank()
                || query.role() == null
                || query.role().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}

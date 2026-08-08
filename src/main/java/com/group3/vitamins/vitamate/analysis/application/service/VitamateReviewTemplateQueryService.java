package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateReviewTemplateReaderPort;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateReviewTemplateListResult;
import com.group3.vitamins.vitamate.analysis.application.usecase.GetVitamateReviewTemplatesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 비타메이트 검토 템플릿 마스터를 조회합니다.
@Service
@RequiredArgsConstructor
public class VitamateReviewTemplateQueryService implements GetVitamateReviewTemplatesUseCase {

    private final VitamateReviewTemplateReaderPort templateReader;

    // 활성화된 검토 유형과 카테고리 목록을 반환합니다.
    @Override
    @Transactional(readOnly = true)
    public VitamateReviewTemplateListResult handle() {
        return VitamateReviewTemplateListResult.from(templateReader.findActiveReviewTemplateGroups());
    }
}

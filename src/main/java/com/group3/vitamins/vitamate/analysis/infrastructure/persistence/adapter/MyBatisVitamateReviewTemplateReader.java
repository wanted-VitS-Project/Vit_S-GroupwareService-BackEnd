package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateReviewTemplateReaderPort;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.mapper.VitamateReviewTemplateMapper;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row.VitamateReviewTemplateGroupRow;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row.VitamateReviewTemplateRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 비타메이트 검토 템플릿 조회 포트를 MyBatis로 구현합니다.
@Component
@RequiredArgsConstructor
public class MyBatisVitamateReviewTemplateReader implements VitamateReviewTemplateReaderPort {

    private final VitamateReviewTemplateMapper mapper;

    // 활성 검토 유형과 하위 카테고리 템플릿을 그룹으로 묶어 반환합니다.
    @Override
    public List<ReviewTemplateGroup> findActiveReviewTemplateGroups() {
        List<VitamateReviewTemplateGroupRow> typeRows = mapper.findActiveReviewTypes();
        Map<String, List<ReviewTemplate>> templatesByType = mapper.findActiveReviewTemplates().stream()
                .map(this::toTemplate)
                .collect(Collectors.groupingBy(ReviewTemplate::reviewType));

        return typeRows.stream()
                .map(type -> new ReviewTemplateGroup(
                        type.getReviewType(),
                        type.getReviewTypeName(),
                        type.getDescription(),
                        templatesByType.getOrDefault(type.getReviewType(), List.of())
                ))
                .toList();
    }

    // 요청에서 선택한 활성 템플릿만 조회합니다.
    @Override
    public List<ReviewTemplate> findActiveTemplates(String reviewType, List<String> categoryCodes) {
        if (categoryCodes == null || categoryCodes.isEmpty()) {
            return List.of();
        }

        return mapper.findActiveTemplates(reviewType, categoryCodes).stream()
                .map(this::toTemplate)
                .toList();
    }

    // 분석 요청 당시 저장된 템플릿 스냅샷을 조회합니다.
    @Override
    public List<ReviewTemplate> findAnalysisTemplateSnapshots(Long analysisId) {
        return mapper.findAnalysisTemplateSnapshots(analysisId).stream()
                .map(this::toTemplate)
                .toList();
    }

    // MyBatis Row를 application 포트 값으로 변환합니다.
    private ReviewTemplate toTemplate(VitamateReviewTemplateRow row) {
        return new ReviewTemplate(
                row.getReviewType(),
                row.getCategoryCode(),
                row.getCategoryName(),
                row.getGuideText(),
                row.getExampleText(),
                row.getPromptTemplate(),
                row.getTemplateVersion(),
                row.getSortOrder()
        );
    }
}

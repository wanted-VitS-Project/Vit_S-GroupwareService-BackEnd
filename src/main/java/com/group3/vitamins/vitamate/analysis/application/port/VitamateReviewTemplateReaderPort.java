package com.group3.vitamins.vitamate.analysis.application.port;

import java.util.List;

// 비타메이트 검토 템플릿 마스터와 분석 요청 스냅샷을 조회하는 포트입니다.
public interface VitamateReviewTemplateReaderPort {

    // 사용자에게 노출할 활성 검토 유형과 카테고리 템플릿 목록을 조회합니다.
    List<ReviewTemplateGroup> findActiveReviewTemplateGroups();

    // 분석 요청에서 선택한 검토 유형과 카테고리 코드가 활성 템플릿인지 조회합니다.
    List<ReviewTemplate> findActiveTemplates(String reviewType, List<String> categoryCodes);

    // Python worker에 전달할 분석 요청 당시 템플릿 스냅샷을 조회합니다.
    List<ReviewTemplate> findAnalysisTemplateSnapshots(Long analysisId);

    record ReviewTemplateGroup(
            String reviewType,
            String reviewTypeName,
            String description,
            List<ReviewTemplate> templates
    ) {
    }

    record ReviewTemplate(
            String reviewType,
            String categoryCode,
            String categoryName,
            String guideText,
            String exampleText,
            String promptTemplate,
            String templateVersion,
            Integer sortOrder
    ) {
    }
}

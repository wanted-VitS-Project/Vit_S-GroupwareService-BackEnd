package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.mapper;

import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row.VitamateReviewTemplateGroupRow;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row.VitamateReviewTemplateRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 비타메이트 검토 템플릿 마스터와 분석 스냅샷 SQL을 호출하는 Mapper입니다.
@Mapper
public interface VitamateReviewTemplateMapper {

    // 활성 검토 유형 목록을 정렬 순서대로 조회합니다.
    List<VitamateReviewTemplateGroupRow> findActiveReviewTypes();

    // 활성 검토 카테고리 템플릿 목록을 정렬 순서대로 조회합니다.
    List<VitamateReviewTemplateRow> findActiveReviewTemplates();

    // 요청에서 선택한 검토 유형과 카테고리 코드의 활성 템플릿을 조회합니다.
    List<VitamateReviewTemplateRow> findActiveTemplates(
            @Param("reviewType") String reviewType,
            @Param("categoryCodes") List<String> categoryCodes
    );

    // 분석 요청 당시 저장된 템플릿 스냅샷을 조회합니다.
    List<VitamateReviewTemplateRow> findAnalysisTemplateSnapshots(@Param("analysisId") Long analysisId);
}

package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.mapper.VitamateAnalysisMapper;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.mapper.VitamateReviewTemplateMapper;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row.VitamateAnalysisRow;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row.VitamateReviewTemplateRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MyBatisVitamateReader")
class MyBatisVitamateReaderTest {

    private VitamateAnalysisMapper analysisMapper;
    private VitamateReviewTemplateMapper templateMapper;
    private MyBatisVitamateReader reader;

    @BeforeEach
    void setUp() {
        analysisMapper = mock(VitamateAnalysisMapper.class);
        templateMapper = mock(VitamateReviewTemplateMapper.class);
        reader = new MyBatisVitamateReader(analysisMapper, templateMapper);
    }

    @Test
    @DisplayName("분석 상세 조회 시 카테고리별 템플릿 버전 스냅샷을 모두 반환한다")
    void returnsTemplateVersionForEachSelectedCategory() {
        Long analysisId = 1L;
        String userId = "EMP001";

        when(analysisMapper.findAccessibleAnalysis(analysisId, userId))
                .thenReturn(analysisRow(analysisId));
        when(templateMapper.findAnalysisTemplateSnapshots(analysisId))
                .thenReturn(List.of(
                        templateRow("COST_RESULT", "COST_REPORT_V1"),
                        templateRow("COST_OVERVIEW", "COST_REPORT_V2")
                ));
        when(analysisMapper.findAnalysisDocuments(analysisId)).thenReturn(List.of());
        when(analysisMapper.findAnalysisCitations(analysisId)).thenReturn(List.of());

        VitamateAnalysisReaderPort.VitamateAnalysisDetail detail = reader
                .findAccessibleAnalysis(analysisId, userId)
                .orElseThrow();

        assertThat(detail.reviewCategoryCodes())
                .containsExactly("COST_RESULT", "COST_OVERVIEW");
        assertThat(detail.prompt()).isEqualTo("합계를 중심으로 검토해주세요.");
        assertThat(detail.templateVersions())
                .extracting(
                        VitamateAnalysisReaderPort.TemplateVersion::categoryCode,
                        VitamateAnalysisReaderPort.TemplateVersion::templateVersion
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("COST_RESULT", "COST_REPORT_V1"),
                        org.assertj.core.groups.Tuple.tuple("COST_OVERVIEW", "COST_REPORT_V2")
                );
    }

    @Test
    @DisplayName("템플릿 도입 전 분석은 템플릿 버전 빈 목록으로 조회한다")
    void returnsEmptyTemplateVersionsForLegacyAnalysis() {
        Long analysisId = 2L;
        String userId = "EMP001";
        VitamateAnalysisRow row = analysisRow(analysisId);
        row.setReviewType(null);
        row.setReviewCategoryCodes(null);
        row.setPrompt(null);

        when(analysisMapper.findAccessibleAnalysis(analysisId, userId)).thenReturn(row);
        when(templateMapper.findAnalysisTemplateSnapshots(analysisId)).thenReturn(List.of());
        when(analysisMapper.findAnalysisDocuments(analysisId)).thenReturn(List.of());
        when(analysisMapper.findAnalysisCitations(analysisId)).thenReturn(List.of());

        VitamateAnalysisReaderPort.VitamateAnalysisDetail detail = reader
                .findAccessibleAnalysis(analysisId, userId)
                .orElseThrow();

        assertThat(detail.reviewType()).isNull();
        assertThat(detail.reviewCategoryCodes()).isEmpty();
        assertThat(detail.prompt()).isNull();
        assertThat(detail.templateVersions()).isEmpty();
    }

    // 분석 상세 조립 테스트에 필요한 기본 조회 행을 생성합니다.
    private VitamateAnalysisRow analysisRow(Long analysisId) {
        VitamateAnalysisRow row = new VitamateAnalysisRow();
        row.setAnalysisId(analysisId);
        row.setBlockId(10L);
        row.setReviewType("COST_REPORT");
        row.setReviewCategoryCodes("COST_RESULT,COST_OVERVIEW");
        row.setPrompt("합계를 중심으로 검토해주세요.");
        row.setAnalysisStatus("COMPLETED");
        row.setResult("검토 결과");
        row.setCreatedAt(LocalDateTime.of(2026, 8, 7, 12, 0));
        row.setCompletedAt(LocalDateTime.of(2026, 8, 7, 12, 1));
        return row;
    }

    // 카테고리별 템플릿 버전 스냅샷 조회 행을 생성합니다.
    private VitamateReviewTemplateRow templateRow(String categoryCode, String templateVersion) {
        VitamateReviewTemplateRow row = new VitamateReviewTemplateRow();
        row.setCategoryCode(categoryCode);
        row.setTemplateVersion(templateVersion);
        return row;
    }
}

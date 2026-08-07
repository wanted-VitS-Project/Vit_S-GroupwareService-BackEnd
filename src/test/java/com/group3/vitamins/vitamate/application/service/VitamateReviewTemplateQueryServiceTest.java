package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateReviewTemplateReaderPort;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateReviewTemplateListResult;
import com.group3.vitamins.vitamate.analysis.application.service.VitamateReviewTemplateQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("VitamateReviewTemplateQueryService")
class VitamateReviewTemplateQueryServiceTest {

    private VitamateReviewTemplateReaderPort templateReader;
    private VitamateReviewTemplateQueryService queryService;

    @BeforeEach
    void setUp() {
        templateReader = mock(VitamateReviewTemplateReaderPort.class);
        queryService = new VitamateReviewTemplateQueryService(templateReader);
    }

    @Test
    @DisplayName("활성 검토 유형과 카테고리를 응답 구조로 변환한다")
    void returnsActiveReviewTemplateGroups() {
        when(templateReader.findActiveReviewTemplateGroups()).thenReturn(List.of(
                new VitamateReviewTemplateReaderPort.ReviewTemplateGroup(
                        "COST_REPORT",
                        "원가계산보고서 검토",
                        "원가계산보고서를 검토합니다.",
                        List.of(new VitamateReviewTemplateReaderPort.ReviewTemplate(
                                "COST_REPORT",
                                "COST_RESULT",
                                "원가계산 결과",
                                "작성 가이드",
                                "입력 예시",
                                "내부 프롬프트",
                                "COST_REPORT_V1",
                                1
                        ))
                )
        ));

        VitamateReviewTemplateListResult result = queryService.handle();

        assertThat(result.reviewTypes()).hasSize(1);
        assertThat(result.reviewTypes().get(0).reviewType()).isEqualTo("COST_REPORT");
        assertThat(result.reviewTypes().get(0).categories())
                .hasSize(1)
                .first()
                .satisfies(category -> {
                    assertThat(category.categoryCode()).isEqualTo("COST_RESULT");
                    assertThat(category.categoryName()).isEqualTo("원가계산 결과");
                    assertThat(category.guideText()).isEqualTo("작성 가이드");
                    assertThat(category.exampleText()).isEqualTo("입력 예시");
                    assertThat(category.templateVersion()).isEqualTo("COST_REPORT_V1");
                });
    }

    @Test
    @DisplayName("활성 검토 유형이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoReviewTypeExists() {
        when(templateReader.findActiveReviewTemplateGroups()).thenReturn(List.of());

        VitamateReviewTemplateListResult result = queryService.handle();

        assertThat(result.reviewTypes()).isEmpty();
    }
}

package com.group3.vitamins.vitamate.analysis.presentation.api.dto.response;

import com.group3.vitamins.vitamate.analysis.application.result.VitamateReviewTemplateListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 비타메이트 검토 템플릿 목록 응답 DTO입니다.
@Schema(description = "비타메이트 검토 템플릿 목록 응답")
public record VitamateReviewTemplateListResponse(
        @Schema(description = "검토 유형 목록")
        List<ReviewType> reviewTypes
) {

    // application result를 API 응답 DTO로 변환합니다.
    public static VitamateReviewTemplateListResponse from(VitamateReviewTemplateListResult result) {
        return new VitamateReviewTemplateListResponse(
                result.reviewTypes().stream()
                        .map(ReviewType::from)
                        .toList()
        );
    }

    public record ReviewType(
            @Schema(description = "검토 유형 코드", example = "COST_REPORT")
            String reviewType,

            @Schema(description = "검토 유형 이름", example = "원가계산 검토")
            String reviewTypeName,

            @Schema(description = "검토 유형 설명", example = "원가계산 결과와 산출내역을 기준으로 문서를 검토합니다.")
            String description,

            @Schema(description = "검토 카테고리 목록")
            List<Category> categories
    ) {

        // result의 검토 유형 값을 응답 값으로 변환합니다.
        private static ReviewType from(VitamateReviewTemplateListResult.ReviewType result) {
            return new ReviewType(
                    result.reviewType(),
                    result.reviewTypeName(),
                    result.description(),
                    result.categories().stream()
                            .map(Category::from)
                            .toList()
            );
        }
    }

    public record Category(
            @Schema(description = "검토 카테고리 코드", example = "COST_RESULT")
            String categoryCode,

            @Schema(description = "검토 카테고리 이름", example = "I. 원가계산 결과")
            String categoryName,

            @Schema(description = "사용자 작성 가이드", example = "원가 총액과 항목별 합계가 일관되는지 검토합니다.")
            String guideText,

            @Schema(description = "사용자 입력 예시", example = "총괄표와 산출내역의 합계가 일치하는지 확인해주세요.")
            String exampleText,

            @Schema(description = "템플릿 버전", example = "COST_REPORT_V1")
            String templateVersion
    ) {

        // result의 카테고리 값을 응답 값으로 변환합니다.
        private static Category from(VitamateReviewTemplateListResult.Category result) {
            return new Category(
                    result.categoryCode(),
                    result.categoryName(),
                    result.guideText(),
                    result.exampleText(),
                    result.templateVersion()
            );
        }
    }
}

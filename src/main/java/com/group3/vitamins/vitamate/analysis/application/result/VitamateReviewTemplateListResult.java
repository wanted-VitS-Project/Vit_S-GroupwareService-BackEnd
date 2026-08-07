package com.group3.vitamins.vitamate.analysis.application.result;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateReviewTemplateReaderPort;

import java.util.List;

// 사용자에게 제공할 비타메이트 검토 템플릿 목록 결과입니다.
public record VitamateReviewTemplateListResult(
        List<ReviewType> reviewTypes
) {

    // 포트 조회 결과를 application result로 변환합니다.
    public static VitamateReviewTemplateListResult from(
            List<VitamateReviewTemplateReaderPort.ReviewTemplateGroup> groups
    ) {
        return new VitamateReviewTemplateListResult(
                groups.stream()
                        .map(ReviewType::from)
                        .toList()
        );
    }

    public record ReviewType(
            String reviewType,
            String reviewTypeName,
            String description,
            List<Category> categories
    ) {

        // 템플릿 그룹을 응답에 필요한 검토 유형 단위로 변환합니다.
        private static ReviewType from(VitamateReviewTemplateReaderPort.ReviewTemplateGroup group) {
            return new ReviewType(
                    group.reviewType(),
                    group.reviewTypeName(),
                    group.description(),
                    group.templates().stream()
                            .map(Category::from)
                            .toList()
            );
        }
    }

    public record Category(
            String categoryCode,
            String categoryName,
            String guideText,
            String exampleText,
            String templateVersion
    ) {

        // 내부 promptTemplate은 공개하지 않고 사용자 작성 가이드만 반환합니다.
        private static Category from(VitamateReviewTemplateReaderPort.ReviewTemplate template) {
            return new Category(
                    template.categoryCode(),
                    template.categoryName(),
                    template.guideText(),
                    template.exampleText(),
                    template.templateVersion()
            );
        }
    }
}

package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.ProjectCategoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사업 카테고리 연결 응답")
public record ProjectCategoryLinkResponse(

        @Schema(description = "프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "연결 후 전체 카테고리. 방금 추가한 것만이 아니다")
        List<Item> businessCategories
) {

    @Schema(description = "사업 카테고리")
    public record Item(

            @Schema(description = "카테고리 ID", example = "1")
            Long categoryId,

            @Schema(description = "카테고리명", example = "환경")
            String name,

            @Schema(description = "업무코드. 없을 수 있다", example = "ENV", nullable = true)
            String code
    ) {
    }

    public static ProjectCategoryLinkResponse from(ProjectCategoryResult result) {
        return new ProjectCategoryLinkResponse(result.projectId(),
                result.businessCategories().stream()
                        .map(category -> new Item(
                                category.categoryId(), category.name(), category.code()))
                        .toList());
    }
}

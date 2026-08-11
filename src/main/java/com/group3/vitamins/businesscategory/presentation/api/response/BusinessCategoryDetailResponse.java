package com.group3.vitamins.businesscategory.presentation.api.response;

import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사업 카테고리 생성 응답")
public record BusinessCategoryDetailResponse(

        @Schema(description = "카테고리 ID", example = "4")
        Long categoryId,

        @Schema(description = "카테고리 이름", example = "상하수도")
        String name,

        @Schema(description = "업무코드. 미입력이면 null", example = "WATER", nullable = true)
        String code,

        @Schema(description = "설명. 미입력이면 null", example = "상수도·하수도 설계 및 관리", nullable = true)
        String description,

        @Schema(description = "삭제 가능 여부. 생성 직후이므로 항상 true", example = "true")
        boolean deletable,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {

    /** 생성 결과를 응답으로 옮긴다. */
    public static BusinessCategoryDetailResponse from(BusinessCategoryResult result) {
        return new BusinessCategoryDetailResponse(
                result.categoryId(),
                result.name(),
                result.code(),
                result.description(),
                result.deletable(),
                result.createdAt()
        );
    }
}
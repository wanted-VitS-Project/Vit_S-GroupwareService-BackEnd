package com.group3.vitamins.businesscategory.presentation.api.response;

import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사업 카테고리 목록 응답")
public record BusinessCategoryListResponse(

        @Schema(description = "카테고리 목록 (이름 오름차순)")
        List<BusinessCategoryResponse> categories
) {

    /** 조회 결과 목록을 응답 봉투의 data 로 감싼다. */
    public static BusinessCategoryListResponse from(List<BusinessCategoryResult> results) {
        return new BusinessCategoryListResponse(
                results.stream()
                        .map(BusinessCategoryResponse::from)
                        .toList()
        );
    }
}
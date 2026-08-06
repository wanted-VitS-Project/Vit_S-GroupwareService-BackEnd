package com.group3.vitamins.businesscategory.presentation.api.response;

import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사업 카테고리 목록 항목")
public record BusinessCategoryResponse(

        @Schema(description = "카테고리 ID", example = "1")
        Long categoryId,

        @Schema(description = "카테고리 이름", example = "도로 설계")
        String name,

        @Schema(description = "업무코드. 미입력이면 null", example = "ROAD", nullable = true)
        String code,

        @Schema(description = "설명. 미입력이면 null", example = "국도·지방도·고속도로 등 도로 설계 관련 용역 과업",
                nullable = true)
        String description,

        @Schema(description = "삭제 가능 여부. 연결된 프로젝트가 없으면 true", example = "true")
        boolean deletable,

        @Schema(description = "삭제 일시. includeDeleted=true 일 때만 값이 있을 수 있다")
        LocalDateTime deletedAt
) {

    /** 조회 결과를 목록 응답 항목으로 옮긴다. */
    public static BusinessCategoryResponse from(BusinessCategoryResult result) {
        return new BusinessCategoryResponse(
                result.categoryId(),
                result.name(),
                result.code(),
                result.description(),
                result.deletable(),
                result.deletedAt()
        );
    }
}
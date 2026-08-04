package com.group3.vitamins.businesscategory.presentation.api.request;

import com.group3.vitamins.businesscategory.application.command.CreateBusinessCategoryCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사업 카테고리 생성 요청")
public record BusinessCategoryCreateRequest(

        @Schema(description = "카테고리 이름 (최대 100자)", example = "상하수도")
        String name,

        @Schema(description = "업무코드 (최대 30자). 생략하면 null, 빈 문자열은 400", example = "WATER")
        String code,

        @Schema(description = "설명", example = "상수도·하수도 설계 및 관리")
        String description
) {

    /** 요청을 서비스 커맨드로 옮긴다. role 은 세션에서 가져온 값이라 요청 바디에 없다. */
    public CreateBusinessCategoryCommand toCommand(String role) {
        return new CreateBusinessCategoryCommand(name, code, description, role);
    }
}
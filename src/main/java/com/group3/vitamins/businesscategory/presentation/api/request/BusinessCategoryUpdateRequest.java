package com.group3.vitamins.businesscategory.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사업 카테고리 수정 요청 — 보낸 필드만 바뀐다. 셋 다 없으면 400")
public record BusinessCategoryUpdateRequest(

        @Schema(description = "카테고리 이름 (최대 100자). 생략하면 안 바뀐다", example = "상하수도·환경")
        String name,

        @Schema(description = "업무코드 (최대 30자). null 을 보내면 업무코드를 지운다. 생략하면 안 바뀐다",
                example = "WATER", nullable = true)
        String code,

        @Schema(description = "설명. 생략하면 안 바뀐다", example = "상수도·하수도 및 환경 설계")
        String description
) {
}
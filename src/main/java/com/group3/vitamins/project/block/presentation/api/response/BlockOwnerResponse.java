package com.group3.vitamins.project.block.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사번·이름 쌍")
public record BlockOwnerResponse(

        @Schema(description = "사번", example = "E2024001")
        String userId,

        @Schema(description = "이름", example = "김민수")
        String name
) {
}
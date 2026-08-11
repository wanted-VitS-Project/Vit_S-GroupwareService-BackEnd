package com.group3.vitamins.project.block.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사번·이름 쌍")
public record BlockOwnerResponse(

        @Schema(description = "사번", example = "E2024001")
        String userId,

        @Schema(description = "이름", example = "김민수")
        String name,

        @Schema(description = "이 사원이 삭제됐는지. true 면 담당자를 다시 지정해야 한다", example = "false")
        boolean deleted
) {
}
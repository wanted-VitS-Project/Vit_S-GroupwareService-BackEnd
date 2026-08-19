package com.group3.vitamins.major.presentation.api.request;

import com.group3.vitamins.major.application.command.UpdateMajorCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전공 수정 요청")
public record MajorUpdateRequest(
        @Schema(description = "새 전공명(최대 100자 · `,` `;` `:` 줄바꿈 금지 — 엑셀 구분자와 충돌)", example = "소프트웨어공학")
        String name
) {

    public UpdateMajorCommand toCommand(Long majorId, String role) {
        return new UpdateMajorCommand(majorId, name, role);
    }
}

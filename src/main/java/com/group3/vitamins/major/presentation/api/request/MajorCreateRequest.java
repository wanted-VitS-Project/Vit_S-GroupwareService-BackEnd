package com.group3.vitamins.major.presentation.api.request;

import com.group3.vitamins.major.application.command.CreateMajorCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전공 생성 요청")
public record MajorCreateRequest(
        @Schema(description = "전공명(최대 100자)", example = "컴퓨터공학")
        String name
) {

    public CreateMajorCommand toCommand(String role) {
        return new CreateMajorCommand(name, role);
    }
}

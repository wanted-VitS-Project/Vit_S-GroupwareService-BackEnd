package com.group3.vitamins.employeegroup.presentation.api.request;

import com.group3.vitamins.employeegroup.application.command.CreateGroupCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그룹 생성 요청(§2)")
public record CreateGroupRequest(
        @Schema(description = "그룹명(최대 50자)") String name,
        @Schema(description = "설명(최대 500자, 선택)") String description
) {

    public CreateGroupCommand toCommand(String role, String createdBy) {
        return new CreateGroupCommand(role, createdBy, name, description);
    }
}

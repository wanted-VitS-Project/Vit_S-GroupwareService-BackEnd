package com.group3.vitamins.employeegroup.presentation.api.request;

import com.group3.vitamins.employeegroup.application.command.AddMembersCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "구성원 추가 요청(§6)")
public record AddMembersRequest(
        @Schema(description = "추가할 사번 목록(1개 이상)") List<String> userIds
) {

    public AddMembersCommand toCommand(String role, Long groupId) {
        return new AddMembersCommand(role, groupId, userIds);
    }
}

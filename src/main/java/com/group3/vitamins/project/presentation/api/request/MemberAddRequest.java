package com.group3.vitamins.project.presentation.api.request;

import com.group3.vitamins.project.application.command.AddMemberCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 추가 요청")
public record MemberAddRequest(

        @Schema(description = "추가할 사원 사번 (한 명씩)", example = "E2024007")
        String userId,

        @Schema(description = "권한 등급 (VIEWER · EDITOR)", example = "VIEWER")
        String permission
) {

    public AddMemberCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new AddMemberCommand(projectId, userId, permission, requesterUserId, role);
    }
}
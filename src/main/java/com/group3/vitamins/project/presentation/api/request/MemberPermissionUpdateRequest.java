package com.group3.vitamins.project.presentation.api.request;

import com.group3.vitamins.project.application.command.ChangeMemberPermissionCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 권한 변경 요청")
public record MemberPermissionUpdateRequest(

        @Schema(description = "변경할 권한 등급 (VIEWER · EDITOR · NONE)", example = "EDITOR")
        String permission
) {

    public ChangeMemberPermissionCommand toCommand(Long projectId, Long memberId,
                                                   String requesterUserId, String role) {
        return new ChangeMemberPermissionCommand(
                projectId, memberId, permission, requesterUserId, role);
    }
}
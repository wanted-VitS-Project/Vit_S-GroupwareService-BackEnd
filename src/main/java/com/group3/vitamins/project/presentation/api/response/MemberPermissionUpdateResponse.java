package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.MemberResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 권한 변경 응답")
public record MemberPermissionUpdateResponse(

        @Schema(description = "참여자 행 ID", example = "32")
        Long memberId,

        @Schema(description = "사원 사번", example = "E2024007")
        String userId,

        @Schema(description = "변경된 권한 등급", example = "EDITOR")
        String permission
) {

    public static MemberPermissionUpdateResponse from(MemberResult result) {
        return new MemberPermissionUpdateResponse(
                result.memberId(), result.userId(), result.permission());
    }
}
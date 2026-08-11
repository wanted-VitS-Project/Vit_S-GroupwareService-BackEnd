package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.MemberResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 추가 응답")
public record MemberAddResponse(

        @Schema(description = "생성된 참여자 행 ID", example = "32")
        Long memberId,

        @Schema(description = "사원 사번", example = "E2024007")
        String userId,

        @Schema(description = "이름", example = "김동훈")
        String name,

        @Schema(description = "권한 등급", example = "VIEWER")
        String permission
) {

    public static MemberAddResponse from(MemberResult result) {
        return new MemberAddResponse(
                result.memberId(), result.userId(), result.name(), result.permission());
    }
}
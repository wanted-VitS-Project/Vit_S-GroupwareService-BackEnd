package com.group3.vitamins.employee.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 프로필 사진 등록/변경 응답 (`.ai/api/auth.md` §5-1) */
@Schema(description = "프로필 사진 등록/변경 응답")
public record ProfileImageResponse(

        @Schema(description = "저장된 프로필 사진 조회 URL(서빙 엔드포인트 경로)",
                example = "/api/v1/employees/vitas-EMP001/profile-image")
        String profileImageUrl
) {

    public static ProfileImageResponse of(String profileImageUrl) {
        return new ProfileImageResponse(profileImageUrl);
    }
}

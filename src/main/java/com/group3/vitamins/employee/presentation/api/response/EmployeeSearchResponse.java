package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.EmployeeSearchRow;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사원 검색 결과 항목 (결재자 후보)")
public record EmployeeSearchResponse(

        @Schema(description = "사번", example = "EMP001")
        String userId,

        @Schema(description = "이름", example = "김민준")
        String name,

        @Schema(description = "부서명 (동명이인 구분용, 미배정이면 null)", example = "개발팀")
        String department,

        @Schema(description = "직급명 (동명이인 구분용, 미배정이면 null)", example = "대리")
        String position,

        @Schema(description = "프로필 사진 서빙 경로. 사진 없으면 null (프론트는 null 이면 이니셜 아바타)",
                example = "/api/v1/employees/vitas-EMP001/profile-image")
        String profileImageUrl
) {

    /** 조회 결과를 응답 항목으로 옮긴다. */
    public static EmployeeSearchResponse from(EmployeeSearchRow row) {
        return new EmployeeSearchResponse(
                row.userId(),
                row.name(),
                row.department(),
                row.position(),
                profileImageUrl(row.userId(), row.profileImageKey())
        );
    }

    /**
     * 아바타 서빙 경로. 키가 없으면 null 이라 프론트가 호출 자체를 건너뛴다.
     * presigned URL 이 아니라 <b>안 만료되는 우리 경로</b>다 — 서명·만료는 서빙 API 가 책임진다 (`employee.md` §10).
     */
    private static String profileImageUrl(String userId, String profileImageKey) {
        return profileImageKey == null ? null : "/api/v1/employees/" + userId + "/profile-image";
    }
}

package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.MemberSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "참여자 목록 조회 응답")
public record MemberListResponse(

        @Schema(description = "참여자 목록 (이름 오름차순, 동명이인은 사번 오름차순)")
        List<MemberItemResponse> members
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static MemberListResponse from(List<MemberSummary> summaries) {
        return new MemberListResponse(summaries.stream()
                .map(MemberItemResponse::from)
                .toList());
    }

    @Schema(description = "참여자")
    public record MemberItemResponse(

            @Schema(description = "project_member 행 ID", example = "31")
            Long memberId,

            @Schema(description = "사원 사번", example = "E2024001")
            String userId,

            @Schema(description = "이름", example = "김용준")
            String name,

            @Schema(description = "부서명. 부서 미배정이면 null", example = "사업1팀")
            String department,

            @Schema(description = "프로젝트 권한", example = "EDITOR",
                    allowableValues = {"VIEWER", "EDITOR", "NONE"})
            String permission,

            @Schema(description = "퇴사자 여부 (배지 표시용)", example = "false")
            boolean resigned
    ) {

        static MemberItemResponse from(MemberSummary summary) {
            return new MemberItemResponse(
                    summary.memberId(), summary.userId(), summary.name(),
                    summary.department(), summary.permission(), summary.resigned());
        }
    }
}
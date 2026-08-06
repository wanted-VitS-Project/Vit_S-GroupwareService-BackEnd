package com.group3.vitamins.project.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.application.query.MemberListQuery;
import com.group3.vitamins.project.application.result.MemberSummary;
import com.group3.vitamins.project.application.usecase.ProjectMemberQueryUseCase;
import com.group3.vitamins.project.presentation.api.response.MemberListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ProjectMember - 프로젝트 참여자", description = "참여자 조회 / 추가 / 권한 변경 / 제거 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberQueryUseCase projectMemberQueryUseCase;

    @Operation(summary = "참여자 목록 조회",
            description = "프로젝트 참여자를 이름 오름차순(동명이인은 사번 오름차순)으로 조회한다. "
                    + "permission 이 NONE 인 참여자도 포함된다 — 참여자 관리 화면에서 권한을 되돌려야 하기 때문이다. "
                    + "부서가 배정되지 않은 사원은 department 가 null 이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_ACCESS_DENIED — 프로젝트 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 존재하지 않음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<MemberListResponse>> getMembers(
            @Parameter(description = "조회할 프로젝트 ID")
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        List<MemberSummary> members = projectMemberQueryUseCase.getMembers(new MemberListQuery(
                projectId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        MemberListResponse.from(members)));
    }
}
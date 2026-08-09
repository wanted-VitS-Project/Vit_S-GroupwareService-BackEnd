package com.group3.vitamins.project.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.application.command.RemoveMemberCommand;
import com.group3.vitamins.project.application.query.MemberListQuery;
import com.group3.vitamins.project.application.result.MemberResult;
import com.group3.vitamins.project.application.result.MemberSummary;
import com.group3.vitamins.project.application.usecase.ProjectMemberCommandUseCase;
import com.group3.vitamins.project.application.usecase.ProjectMemberQueryUseCase;
import com.group3.vitamins.project.presentation.api.request.MemberAddRequest;
import com.group3.vitamins.project.presentation.api.request.MemberPermissionUpdateRequest;
import com.group3.vitamins.project.presentation.api.response.MemberAddResponse;
import com.group3.vitamins.project.presentation.api.response.MemberListResponse;
import com.group3.vitamins.project.presentation.api.response.MemberPermissionUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ProjectMember - 프로젝트 참여자", description = "참여자 조회 / 추가 / 권한 변경 / 제거 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberQueryUseCase projectMemberQueryUseCase;
    private final ProjectMemberCommandUseCase projectMemberCommandUseCase;

    @Operation(summary = "참여자 목록 조회",
            description = "프로젝트 참여자를 이름 오름차순(동명이인은 사번 오름차순)으로 조회한다. "
                    + "permission 은 VIEWER · EDITOR 두 값뿐이다 — 차단은 참여자 제거로 표현한다. "
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

    @Operation(summary = "참여자 추가",
            description = "참여자를 한 명씩 추가한다. 팀·부서 일괄 추가는 지원하지 않는다(PRJ-009 · INV-07). "
                    + "permission 은 VIEWER · EDITOR 만 허용된다. NONE 을 보내면 400 이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "참여자 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "MEMBER_PERMISSION_INVALID — 허용되지 않은 권한 등급"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND / USER_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "MEMBER_ALREADY_EXISTS — 이미 참여자로 등록됨")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<MemberAddResponse>> addMember(
            @Parameter(description = "참여자를 추가할 프로젝트 ID")
            @PathVariable Long projectId,
            @RequestBody MemberAddRequest request,
            Authentication authentication
    ) {
        MemberResult result = projectMemberCommandUseCase.addMember(request.toCommand(
                projectId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        MemberAddResponse.from(result)));
    }

    @Operation(summary = "참여자 권한 변경",
            description = "참여자 권한을 VIEWER · EDITOR 로 바꾼다. NONE 을 보내면 400 이다. "
                    + "자기 자신의 권한 행은 EDITOR 여도 변경할 수 없다(PRJ-011 · INV-10).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "참여자 권한 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "MEMBER_PERMISSION_INVALID — 허용되지 않은 권한 등급"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "MEMBER_SELF_EDIT_DENIED / PROJECT_EDIT_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MEMBER_NOT_FOUND — 참여자가 존재하지 않음")
    })
    @PatchMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberPermissionUpdateResponse>> changePermission(
            @Parameter(description = "프로젝트 ID")
            @PathVariable Long projectId,
            @Parameter(description = "참여자 행 ID")
            @PathVariable Long memberId,
            @RequestBody MemberPermissionUpdateRequest request,
            Authentication authentication
    ) {
        MemberResult result = projectMemberCommandUseCase.changePermission(request.toCommand(
                projectId, memberId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        MemberPermissionUpdateResponse.from(result)));
    }

    @Operation(summary = "참여자 제거",
            description = "참여자를 프로젝트에서 제거한다. 자기 자신은 제거할 수 없다. "
                    + "project_member 는 소프트 삭제 컬럼이 없어 행이 물리적으로 삭제된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "참여자 제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "MEMBER_SELF_EDIT_DENIED / PROJECT_EDIT_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MEMBER_NOT_FOUND — 참여자가 존재하지 않음")
    })
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @Parameter(description = "프로젝트 ID")
            @PathVariable Long projectId,
            @Parameter(description = "참여자 행 ID")
            @PathVariable Long memberId,
            Authentication authentication
    ) {
        projectMemberCommandUseCase.removeMember(new RemoveMemberCommand(
                projectId, memberId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success(ProjectResponseMessage.SUCCESS));
    }
}
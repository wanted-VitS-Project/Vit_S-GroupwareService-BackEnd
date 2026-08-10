package com.group3.vitamins.project.step.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import com.group3.vitamins.project.step.application.command.RevokeStepPermissionCommand;
import com.group3.vitamins.project.step.application.query.StepPermissionListQuery;
import com.group3.vitamins.project.step.application.result.StepPermissionResult;
import com.group3.vitamins.project.step.application.result.StepPermissionSummary;
import com.group3.vitamins.project.step.application.usecase.StepPermissionUseCase;
import com.group3.vitamins.project.step.presentation.api.request.StepPermissionRequest;
import com.group3.vitamins.project.step.presentation.api.response.StepPermissionListResponse;
import com.group3.vitamins.project.step.presentation.api.response.StepPermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "StepPermission - 스텝 권한", description = "스텝 권한 오버라이드 조회 / 부여 / 회수 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/steps/{stepId}/permissions")
@RequiredArgsConstructor
public class StepPermissionController {

    private final StepPermissionUseCase stepPermissionUseCase;

    @Operation(summary = "스텝 권한 목록 조회",
            description = "프로젝트 참여자 전원의 스텝 권한 판정을 조회한다. "
                    + "overridden 이 false 면 step_permission 행이 없다는 뜻이고 값은 프로젝트 권한 상속이다(STP-011). "
                    + "권한은 프로젝트 EDITOR 기준이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND — 스텝이 존재하지 않음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<StepPermissionListResponse>> getPermissions(
            @Parameter(description = "조회할 스텝 ID")
            @PathVariable Long stepId,
            Authentication authentication
    ) {
        List<StepPermissionSummary> permissions = stepPermissionUseCase.getPermissions(
                new StepPermissionListQuery(stepId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StepPermissionListResponse.from(permissions)));
    }

    @Operation(summary = "스텝 권한 부여·변경",
            description = "스텝별 VIEWER / EDITOR / NONE 을 지정한다. "
                    + "특정 스텝만 가리려면 NONE 행을 명시적으로 넣어야 한다 — 행이 없으면 차단이 아니라 상속이다(STP-011). "
                    + "자기 자신의 권한 행은 수정할 수 없다(INV-10).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "권한 설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STEP_PERMISSION_INVALID — 허용되지 않은 권한 등급"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "MEMBER_SELF_EDIT_DENIED / PROJECT_EDIT_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND / USER_NOT_FOUND")
    })
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<StepPermissionResponse>> setPermission(
            @Parameter(description = "스텝 ID")
            @PathVariable Long stepId,
            @Parameter(description = "대상 사원 사번")
            @PathVariable String userId,
            @Valid @RequestBody StepPermissionRequest request,
            Authentication authentication
    ) {
        StepPermissionResult result = stepPermissionUseCase.setPermission(
                request.toCommand(stepId, userId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StepPermissionResponse.from(result)));
    }

    @Operation(summary = "스텝 권한 회수",
            description = "오버라이드 행을 지워 프로젝트 권한 상속으로 되돌린다(STP-011). 차단이 아니다. "
                    + "응답의 permission 은 회수 후 상속된 등급이다. "
                    + "자기 자신의 행은 회수할 수 없다 (INV-10).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "권한 회수 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "MEMBER_SELF_EDIT_DENIED — 자기 자신의 권한은 회수할 수 없음 "
                            + "/ PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND / STEP_PERMISSION_NOT_FOUND")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<StepPermissionResponse>> revokePermission(
            @Parameter(description = "스텝 ID")
            @PathVariable Long stepId,
            @Parameter(description = "대상 사원 사번")
            @PathVariable String userId,
            Authentication authentication
    ) {
        StepPermissionResult result = stepPermissionUseCase.revokePermission(
                new RevokeStepPermissionCommand(stepId, userId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StepPermissionResponse.from(result)));
    }
}

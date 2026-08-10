package com.group3.vitamins.project.stage.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import com.group3.vitamins.project.stage.application.command.DeleteStageCommand;
import com.group3.vitamins.project.stage.application.result.StageDeleteResult;
import com.group3.vitamins.project.stage.application.result.StageResult;
import com.group3.vitamins.project.stage.application.result.StageStepPermissionResult;
import com.group3.vitamins.project.stage.application.usecase.StageCommandUseCase;
import com.group3.vitamins.project.stage.application.usecase.StageStepPermissionUseCase;
import com.group3.vitamins.project.stage.presentation.api.request.StageStepPermissionRequest;
import com.group3.vitamins.project.stage.presentation.api.request.StageUpdateRequest;
import com.group3.vitamins.project.stage.presentation.api.response.StageDeleteResponse;
import com.group3.vitamins.project.stage.presentation.api.response.StageStepPermissionResponse;
import com.group3.vitamins.project.stage.presentation.api.response.StageUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage - 스테이지", description = "스테이지 생성 / 조회 / 수정 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageCommandUseCase stageCommandUseCase;
    private final StageStepPermissionUseCase stageStepPermissionUseCase;

    @Operation(summary = "스테이지 수정",
            description = "스테이지 이름을 수정한다. 순서 변경은 "
                    + "PATCH /projects/{projectId}/stages/order 소관이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STAGE_NAME_REQUIRED / STAGE_NAME_TOO_LONG"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STAGE_NOT_FOUND — 스테이지가 존재하지 않음")
    })
    @PatchMapping("/{stageId}")
    public ResponseEntity<ApiResponse<StageUpdateResponse>> updateStage(
            @Parameter(description = "수정할 스테이지 ID")
            @PathVariable Long stageId,
            @Valid @RequestBody StageUpdateRequest request,
            Authentication authentication
    ) {
        StageResult result = stageCommandUseCase.updateStage(
                request.toCommand(stageId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StageUpdateResponse.from(result)));
    }

    @Operation(summary = "스테이지 삭제",
            description = "스테이지를 논리 삭제하고 하위 스텝을 지정한 대상으로 이전한다(STG-003). "
                    + "⛔ 스텝은 함께 삭제되지 않는다. moveToStageId 가 0 이면 미소속으로 뺀다. "
                    + "이전된 스텝의 권한은 그대로 유지된다 — 위치와 권한은 독립이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STAGE_MOVE_TARGET_REQUIRED / STAGE_MOVE_TARGET_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STAGE_NOT_FOUND — 스테이지가 존재하지 않음")
    })
    @DeleteMapping("/{stageId}")
    public ResponseEntity<ApiResponse<StageDeleteResponse>> deleteStage(
            @Parameter(description = "삭제할 스테이지 ID")
            @PathVariable Long stageId,
            @Parameter(description = "하위 스텝을 옮길 스테이지 ID. 0 이면 미소속으로 이전")
            @RequestParam(required = false) Long moveToStageId,
            Authentication authentication
    ) {
        StageDeleteResult result = stageCommandUseCase.deleteStage(new DeleteStageCommand(
                stageId, moveToStageId, authentication.getName(),
                RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StageDeleteResponse.from(result)));
    }

    @Operation(summary = "하위 스텝 권한 일괄 적용",
            description = "① 이 스테이지의 새 스텝 권한 기본값을 저장하고, "
                    + "② applyToExistingSteps 가 true 면 기존 하위 스텝에도 지금 적용한다(STG-004). "
                    + "⚠️ 기본값은 권한 판정에 쓰이지 않는다 — 스텝 생성 시 step_permission 으로 복사될 뿐이라 "
                    + "스텝을 다른 스테이지로 옮겨도 권한이 따라 바뀌지 않는다. "
                    + "미소속 스텝에는 기본값이 없어 프로젝트 권한을 상속한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "적용 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STEP_PERMISSION_INVALID — 허용되지 않은 권한 등급"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "MEMBER_SELF_EDIT_DENIED / PROJECT_EDIT_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STAGE_NOT_FOUND / USER_NOT_FOUND")
    })
    @PostMapping("/{stageId}/step-permissions")
    public ResponseEntity<ApiResponse<StageStepPermissionResponse>> applyStepPermissions(
            @Parameter(description = "스테이지 ID")
            @PathVariable Long stageId,
            @Valid @RequestBody StageStepPermissionRequest request,
            Authentication authentication
    ) {
        StageStepPermissionResult result = stageStepPermissionUseCase.apply(
                request.toCommand(stageId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        StageStepPermissionResponse.from(result)));
    }
}

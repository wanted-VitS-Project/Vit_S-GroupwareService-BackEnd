package com.group3.vitamins.project.step.presentation;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import com.group3.vitamins.project.step.application.result.StepResult;
import com.group3.vitamins.project.step.application.usecase.StepCommandUseCase;
import com.group3.vitamins.project.step.presentation.api.request.StepCreateRequest;
import com.group3.vitamins.project.step.presentation.api.response.StepCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Step - 스텝", description = "스텝 생성 / 조회 / 수정 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/steps")
@RequiredArgsConstructor
public class StepController {

    private final StepCommandUseCase stepCommandUseCase;

    @Operation(summary = "스텝 생성",
            description = "프로젝트에 스텝을 추가한다. stageId 를 생략하면 미소속 스텝이 된다. "
                    + "상태는 NOT_STARTED 로 고정되고 sortOrder 는 프로젝트 전체 기준 max+1 로 채워진다. "
                    + "ownerUserId 는 책임자 사번이며 작업자가 아니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STEP_NAME_REQUIRED / STEP_NAME_TOO_LONG / STEP_DATE_RANGE_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND / STAGE_NOT_FOUND / USER_NOT_FOUND")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<StepCreateResponse>> createStep(
            @Parameter(description = "스텝을 추가할 프로젝트 ID")
            @PathVariable Long projectId,
            @RequestBody StepCreateRequest request,
            Authentication authentication
    ) {
        StepResult result = stepCommandUseCase.createStep(request.toCommand(
                projectId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        StepCreateResponse.from(result)));
    }
}
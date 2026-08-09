package com.group3.vitamins.project.step.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import com.group3.vitamins.project.step.application.query.StepListQuery;
import com.group3.vitamins.project.step.application.result.StepResult;
import com.group3.vitamins.project.step.application.result.StepSummary;
import com.group3.vitamins.project.step.application.usecase.StepCommandUseCase;
import com.group3.vitamins.project.step.application.usecase.StepQueryUseCase;
import com.group3.vitamins.project.step.domain.model.StepStatus;
import com.group3.vitamins.project.step.presentation.api.request.StepCreateRequest;
import com.group3.vitamins.project.step.presentation.api.response.StepCreateResponse;
import com.group3.vitamins.project.step.presentation.api.response.StepListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Step - 스텝", description = "스텝 생성 / 조회 / 수정 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/steps")
@RequiredArgsConstructor
public class ProjectStepController {

    private final StepCommandUseCase stepCommandUseCase;
    private final StepQueryUseCase stepQueryUseCase;

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
            @Valid @RequestBody StepCreateRequest request,
            Authentication authentication
    ) {
        StepResult result = stepCommandUseCase.createStep(request.toCommand(
                projectId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        StepCreateResponse.from(result)));
    }

    @Operation(summary = "스텝 목록 조회",
            description = "프로젝트의 스텝을 sortOrder 오름차순으로 조회한다. "
                    + "step_permission 이 NONE 인 스텝은 목록에서 제외된다(STP-010) — 사용자마다 보이는 개수가 다르다. "
                    + "이슈가 0개인 스텝은 progressRate 를 담지 않는다.")
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
    public ResponseEntity<ApiResponse<StepListResponse>> getSteps(
            @Parameter(description = "조회할 프로젝트 ID")
            @PathVariable Long projectId,
            @Parameter(description = "특정 스테이지의 스텝만 조회. 0 이면 미소속 스텝")
            @RequestParam(required = false) Long stageId,
            @Parameter(description = "스텝 상태 필터")
            @RequestParam(required = false) StepStatus status,
            Authentication authentication
    ) {
        List<StepSummary> steps = stepQueryUseCase.getSteps(new StepListQuery(
                projectId, stageId, status,
                authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StepListResponse.from(steps)));
    }
}
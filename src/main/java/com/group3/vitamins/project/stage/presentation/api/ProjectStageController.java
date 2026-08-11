package com.group3.vitamins.project.stage.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import com.group3.vitamins.project.stage.application.query.StageListQuery;
import com.group3.vitamins.project.stage.application.result.StageOrderResult;
import com.group3.vitamins.project.stage.application.result.StageResult;
import com.group3.vitamins.project.stage.application.result.StageSummary;
import com.group3.vitamins.project.stage.application.usecase.StageCommandUseCase;
import com.group3.vitamins.project.stage.application.usecase.StageQueryUseCase;
import com.group3.vitamins.project.stage.presentation.api.request.StageCreateRequest;
import com.group3.vitamins.project.stage.presentation.api.request.StageOrderRequest;
import com.group3.vitamins.project.stage.presentation.api.response.StageCreateResponse;
import com.group3.vitamins.project.stage.presentation.api.response.StageListResponse;
import com.group3.vitamins.project.stage.presentation.api.response.StageOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Stage - 스테이지", description = "스테이지 생성 / 조회 / 수정 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/stages")
@RequiredArgsConstructor
public class ProjectStageController {

    private final StageCommandUseCase stageCommandUseCase;
    private final StageQueryUseCase stageQueryUseCase;

    @Operation(summary = "스테이지 생성",
            description = "프로젝트에 스테이지를 추가한다. sortOrder 를 생략하면 기존 최대값 + 1 이 들어간다. "
                    + "스테이지는 분류 전용이라 상태를 갖지 않는다 — 권한은 새 스텝용 기본값만 가진다(STG-004).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STAGE_NAME_REQUIRED / STAGE_NAME_TOO_LONG"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 존재하지 않음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<StageCreateResponse>> createStage(
            @Parameter(description = "스테이지를 추가할 프로젝트 ID")
            @PathVariable Long projectId,
            @Valid @RequestBody StageCreateRequest request,
            Authentication authentication
    ) {
        StageResult result = stageCommandUseCase.createStage(request.toCommand(
                projectId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        StageCreateResponse.from(result)));
    }

    @Operation(summary = "스테이지 목록 조회",
            description = "프로젝트의 스테이지를 sortOrder 오름차순으로 조회한다. "
                    + "stepCount 는 요청자가 볼 수 있는 스텝만 센다 — 스텝 목록 조회 결과와 개수가 일치한다.")
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
    public ResponseEntity<ApiResponse<StageListResponse>> getStages(
            @Parameter(description = "조회할 프로젝트 ID")
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        List<StageSummary> stages = stageQueryUseCase.getStages(new StageListQuery(
                projectId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StageListResponse.from(stages)));
    }

    @Operation(summary = "스테이지 순서 변경",
            description = "사이드바 스테이지 순서를 일괄 재정렬한다. sort_order 만 갱신하며 "
                    + "하위 스텝은 건드리지 않는다(STG-002). "
                    + "사이드바 전체의 최종 순서를 보내야 한다 — 일부만 보내면 나머지와 순서가 겹친다. "
                    + "항목마다 조회 시점의 version 을 함께 보내며, 하나라도 어긋나면 "
                    + "요청 전체가 409 로 롤백된다(부분 적용 없음).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "재정렬 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STAGE_ORDER_INVALID / STAGE_VERSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND / STAGE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "STAGE_VERSION_CONFLICT — 목록 중 하나라도 먼저 수정됨")
    })
    @PatchMapping("/order")
    public ResponseEntity<ApiResponse<StageOrderResponse>> reorderStages(
            @Parameter(description = "재정렬할 프로젝트 ID")
            @PathVariable Long projectId,
            @Valid @RequestBody StageOrderRequest request,
            Authentication authentication
    ) {
        List<StageOrderResult> results = stageCommandUseCase.reorderStages(
                request.toCommand(projectId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StageOrderResponse.from(results)));
    }
}

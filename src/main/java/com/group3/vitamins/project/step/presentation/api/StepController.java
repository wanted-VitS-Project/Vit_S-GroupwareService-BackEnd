package com.group3.vitamins.project.step.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import com.group3.vitamins.project.step.application.query.StepDetailQuery;
import com.group3.vitamins.project.step.application.result.StepCompleteResult;
import com.group3.vitamins.project.step.application.result.StepDetailResult;
import com.group3.vitamins.project.step.application.result.StepStatusResult;
import com.group3.vitamins.project.step.application.result.StepUpdateResult;
import com.group3.vitamins.project.step.application.usecase.StepCommandUseCase;
import com.group3.vitamins.project.step.application.usecase.StepQueryUseCase;
import com.group3.vitamins.project.step.presentation.api.request.StepCompleteRequest;
import com.group3.vitamins.project.step.presentation.api.request.StepStatusUpdateRequest;
import com.group3.vitamins.project.step.presentation.api.request.StepUpdateRequest;
import com.group3.vitamins.project.step.presentation.api.response.StepCompleteResponse;
import com.group3.vitamins.project.step.presentation.api.response.StepDetailResponse;
import com.group3.vitamins.project.step.presentation.api.response.StepStatusUpdateResponse;
import com.group3.vitamins.project.step.presentation.api.response.StepUpdateResponse;
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

@Tag(name = "Step - 스텝", description = "스텝 생성 / 조회 / 수정 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/steps")
@RequiredArgsConstructor
public class StepController {

    private final StepQueryUseCase stepQueryUseCase;
    private final StepCommandUseCase stepCommandUseCase;

    @Operation(summary = "스텝 상세 조회",
            description = "스텝 기본 정보·이슈 집계·완료 정보를 조회한다. "
                    + "step_permission 이 NONE 이면 403 이다 — 프로젝트 참여자가 아닌 경우도 같은 코드로 거부한다. "
                    + "이슈가 0개면 progressRate 를 응답에 담지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_ACCESS_DENIED — 스텝 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND — 스텝이 없거나 삭제됨")
    })
    @GetMapping("/{stepId}")
    public ResponseEntity<ApiResponse<StepDetailResponse>> getStepDetail(
            @Parameter(description = "조회할 스텝 ID")
            @PathVariable Long stepId,
            Authentication authentication
    ) {
        StepDetailResult result = stepQueryUseCase.getStepDetail(new StepDetailQuery(
                stepId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StepDetailResponse.from(result)));
    }


    @Operation(summary = "스텝 수정",
            description = "이름·기간·책임자를 수정한다. 편집 화면의 폼 전체를 보내며, "
                    + "보내지 않은 필드는 비워진다. "
                    + "소속 스테이지·순서 변경은 PATCH /projects/{projectId}/steps/order 소관이다. "
                    + "상태 변경·완료 처리도 각각 별도 API 다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STEP_NAME_REQUIRED / STEP_NAME_TOO_LONG / STEP_DATE_RANGE_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_EDIT_DENIED — 스텝 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND / USER_NOT_FOUND")
    })
    @PatchMapping("/{stepId}")
    public ResponseEntity<ApiResponse<StepUpdateResponse>> updateStep(
            @Parameter(description = "수정할 스텝 ID")
            @PathVariable Long stepId,
            @Valid @RequestBody StepUpdateRequest request,
            Authentication authentication
    ) {
        StepUpdateResult result = stepCommandUseCase.updateStep(
                request.toCommand(stepId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StepUpdateResponse.from(result)));
    }

    @Operation(summary = "스텝 상태 변경",
            description = "NOT_STARTED · IN_PROGRESS 로만 바꿀 수 있다. "
                    + "DONE 은 미완료 이슈 처리 선택이 필요해 완료 처리 API 를 써야 한다(STP-006). "
                    + "스텝 상태는 진척률과 별개 값이다(STP-004) — 이슈를 다 끝내도 자동으로 완료되지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "STEP_STATUS_INVALID — 허용되지 않은 상태 값 (DONE 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_EDIT_DENIED — 스텝 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND — 스텝이 존재하지 않음")
    })
    @PatchMapping("/{stepId}/status")
    public ResponseEntity<ApiResponse<StepStatusUpdateResponse>> changeStatus(
            @Parameter(description = "상태를 바꿀 스텝 ID")
            @PathVariable Long stepId,
            @Valid @RequestBody StepStatusUpdateRequest request,
            Authentication authentication
    ) {
        StepStatusResult result = stepCommandUseCase.changeStatus(
                request.toCommand(stepId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StepStatusUpdateResponse.from(result)));
    }

    @Operation(summary = "스텝 완료 처리",
            description = "스텝을 DONE 으로 바꾸고 완료자·완료시각을 기록한다. "
                    + "미완료 이슈가 남아 있어도 완료를 막지 않는다(STP-005). "
                    + "KEEP 이면 이슈를 그대로 두고, CLOSE 면 남은 이슈를 함께 완료한다. "
                    + "이미 완료된 스텝은 완료자·완료시각을 덮어쓰지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "완료 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "OPEN_ISSUE_ACTION_REQUIRED / OPEN_ISSUE_ACTION_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_EDIT_DENIED — 스텝 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND — 스텝이 존재하지 않음")
    })
    @PostMapping("/{stepId}/complete")
    public ResponseEntity<ApiResponse<StepCompleteResponse>> completeStep(
            @Parameter(description = "완료 처리할 스텝 ID")
            @PathVariable Long stepId,
            @Valid @RequestBody StepCompleteRequest request,
            Authentication authentication
    ) {
        StepCompleteResult result = stepCommandUseCase.completeStep(
                request.toCommand(stepId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        StepCompleteResponse.from(result)));
    }
}

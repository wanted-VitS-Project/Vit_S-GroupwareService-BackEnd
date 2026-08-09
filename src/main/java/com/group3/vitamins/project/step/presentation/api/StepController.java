package com.group3.vitamins.project.step.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import com.group3.vitamins.project.step.application.query.StepDetailQuery;
import com.group3.vitamins.project.step.application.result.StepDetailResult;
import com.group3.vitamins.project.step.application.usecase.StepQueryUseCase;
import com.group3.vitamins.project.step.presentation.api.response.StepDetailResponse;
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
import com.group3.vitamins.project.step.application.result.StepUpdateResult;
import com.group3.vitamins.project.step.application.usecase.StepCommandUseCase;
import com.group3.vitamins.project.step.presentation.api.request.StepUpdateRequest;
import com.group3.vitamins.project.step.presentation.api.response.StepUpdateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}
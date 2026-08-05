package com.group3.vitamins.jobposition.presentation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.jobposition.application.command.DeleteJobPositionCommand;
import com.group3.vitamins.jobposition.application.command.UpdateJobPositionCommand;
import com.group3.vitamins.jobposition.application.query.JobPositionListQuery;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import com.group3.vitamins.jobposition.application.usecase.JobPositionCommandUseCase;
import com.group3.vitamins.jobposition.application.usecase.JobPositionQueryUseCase;
import com.group3.vitamins.jobposition.presentation.api.request.JobPositionCreateRequest;
import com.group3.vitamins.jobposition.presentation.api.request.JobPositionUpdateRequest;
import com.group3.vitamins.jobposition.presentation.api.response.JobPositionDetailResponse;
import com.group3.vitamins.jobposition.presentation.api.response.JobPositionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "JobPosition - 직급", description = "직급 조회 / 생성 / 수정 / 삭제 — 전부 ADMIN 전용 (담당: 김동현)")
@RestController
@RequestMapping("/api/v1/job-positions")
@RequiredArgsConstructor
public class JobPositionController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JobPositionQueryUseCase jobPositionQueryUseCase;
    private final JobPositionCommandUseCase jobPositionCommandUseCase;

    @Operation(summary = "직급 목록 조회",
            description = "직급을 정렬 순서 오름차순(같으면 직급명 오름차순)으로 조회한다. 페이징·검색 파라미터는 없다. "
                    + "각 항목의 employeeCount 는 시스템 계정·퇴사자를 제외한 사용 인원이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공 (0건이면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님 (MASTER 포함)")
    })
    @GetMapping
    public ApiResponse<JobPositionListResponse> listJobPositions(Authentication authentication) {
        JobPositionListResponse data = JobPositionListResponse.from(
                jobPositionQueryUseCase.listJobPositions(
                        new JobPositionListQuery(currentRole(authentication))));

        return ApiResponse.success(JobPositionResponseMessage.LIST_SUCCESS, data);
    }

    @Operation(summary = "직급 생성",
            description = "직급명으로 직급을 생성한다. name 은 필수·중복 불가(최대 30자). sortOrder 를 생략하면 마지막 순서 + 1 이 된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "POS_INVALID_REQUEST — 직급명이 비었거나 30자 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "POS_NAME_DUPLICATED — 이미 존재하는 직급명")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<JobPositionDetailResponse> createJobPosition(
            @RequestBody JobPositionCreateRequest request,
            Authentication authentication) {

        JobPositionResult result = jobPositionCommandUseCase.createJobPosition(
                request.toCommand(currentRole(authentication)));

        return ApiResponse.created(JobPositionResponseMessage.CREATED,
                JobPositionDetailResponse.from(result));
    }

    @Operation(summary = "직급 수정",
            description = "보낸 필드만 바꾼다(직급명 수정·순서 변경이 같은 API). name·sortOrder 둘 다 없으면 400. "
                    + "순서는 UNIQUE 가 아니라 서로 맞바꿀 때 두 번 호출해 중간에 값이 겹쳐도 오류가 나지 않는다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(implementation = JobPositionUpdateRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "POS_INVALID_REQUEST — 수정할 필드 없음 또는 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "POS_NOT_FOUND — 직급 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "POS_NAME_DUPLICATED — 이미 존재하는 직급명")
    })
    @PatchMapping("/{jobPositionId}")
    public ApiResponse<JobPositionDetailResponse> updateJobPosition(
            @Parameter(description = "수정할 직급 번호")
            @PathVariable Long jobPositionId,
            @RequestBody JsonNode requestBody,
            Authentication authentication) {

        JobPositionResult result = jobPositionCommandUseCase.updateJobPosition(
                toUpdateCommand(jobPositionId, requestBody, currentRole(authentication)));

        return ApiResponse.success(JobPositionResponseMessage.UPDATED,
                JobPositionDetailResponse.from(result));
    }

    @Operation(summary = "직급 삭제",
            description = "사용 인원이 없으면 직급을 삭제한다(하드 삭제). 사용 인원이 있으면 409 — 먼저 사원의 직급을 바꾸거나 비워야 한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "POS_NOT_FOUND — 직급 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "POS_IN_USE — 사용 인원 있음 (인원 수는 message 문구에 포함)")
    })
    @DeleteMapping("/{jobPositionId}")
    public ApiResponse<Void> deleteJobPosition(
            @Parameter(description = "삭제할 직급 번호")
            @PathVariable Long jobPositionId,
            Authentication authentication) {

        jobPositionCommandUseCase.deleteJobPosition(
                new DeleteJobPositionCommand(jobPositionId, currentRole(authentication)));

        return ApiResponse.success(JobPositionResponseMessage.DELETED);
    }

    /** 세션 권한(ROLE_ADMIN 형태)에서 전역 role 문자열을 꺼낸다. */
    private String currentRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .findFirst()
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .orElse("");
    }

    /**
     * raw JSON 에서 필드 존재 여부(생략 vs 값 전달)를 직접 확인해 커맨드로 옮긴다.
     * {@code "sortOrder": null} 은 "전달 안 함" 으로 취급한다 — sortOrder 는 지울 수 있는 값이 아니다.
     */
    private UpdateJobPositionCommand toUpdateCommand(Long jobPositionId, JsonNode body, String role) {
        boolean nameProvided = body.has("name");
        boolean sortOrderProvided = body.has("sortOrder") && !body.get("sortOrder").isNull();

        String name = nameProvided ? textOrNull(body, "name") : null;
        Integer sortOrder = sortOrderProvided ? body.get("sortOrder").asInt() : null;

        return new UpdateJobPositionCommand(
                jobPositionId, nameProvided, name, sortOrderProvided, sortOrder, role);
    }

    private String textOrNull(JsonNode body, String field) {
        JsonNode value = body.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }
}

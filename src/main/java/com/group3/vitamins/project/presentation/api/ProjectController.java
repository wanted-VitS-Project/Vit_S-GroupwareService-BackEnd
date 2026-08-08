package com.group3.vitamins.project.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.application.query.ProjectDetailQuery;
import com.group3.vitamins.project.application.query.ProjectListQuery;
import com.group3.vitamins.project.application.query.ProjectProgressQuery;
import com.group3.vitamins.project.application.result.ProjectDetailResult;
import com.group3.vitamins.project.application.result.ProjectPageResult;
import com.group3.vitamins.project.application.result.ProjectProgressResult;
import com.group3.vitamins.project.application.result.ProjectResult;
import com.group3.vitamins.project.application.usecase.ProjectCommandUseCase;
import com.group3.vitamins.project.application.usecase.ProjectQueryUseCase;
import com.group3.vitamins.project.presentation.api.request.ProjectCreateRequest;
import com.group3.vitamins.project.presentation.api.response.ProjectCreateResponse;
import com.group3.vitamins.project.presentation.api.response.ProjectDetailResponse;
import com.group3.vitamins.project.presentation.api.response.ProjectListResponse;
import com.group3.vitamins.project.presentation.api.response.ProjectProgressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Project - 프로젝트", description = "프로젝트 생성 / 조회 / 수정 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectCommandUseCase projectCommandUseCase;
    private final ProjectQueryUseCase projectQueryUseCase;

    @Operation(summary = "프로젝트 생성",
            description = "프로젝트를 생성한다. bidNoticeId(선택)를 보내면 공고와 연결된 채로 생성한다. "
                    + "상태는 NOT_STARTED 로 고정되고, 생성자는 자동으로 EDITOR 참여자가 된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "PROJECT_NAME_REQUIRED / _NAME_TOO_LONG / _DATE_RANGE_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "BUSINESS_CATEGORY_NOT_FOUND — 사업 카테고리가 존재하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "PROJECT_BID_NOTICE_ALREADY_LINKED — 이미 다른 프로젝트가 연결된 공고")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> createProject(
            @RequestBody ProjectCreateRequest request,
            Authentication authentication
    ) {
        ProjectResult result = projectCommandUseCase.createProject(
                request.toCommand(authentication.getName()));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        ProjectCreateResponse.from(result)));
    }

    @Operation(summary = "프로젝트 상세 조회",
            description = "프로젝트 기본 정보·진척률·사업 카테고리를 조회한다. "
                    + "myPermission 으로 FE 가 편집·삭제 버튼을 게이팅한다. "
                    + "스텝이 0개면 progressRate 를 응답에 담지 않는다 — 0% 로 그리지 마라.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_ACCESS_DENIED — 프로젝트 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 없거나 삭제됨")
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetail(
            @Parameter(description = "조회할 프로젝트 ID")
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        ProjectDetailResult result = projectQueryUseCase.getProjectDetail(
                new ProjectDetailQuery(projectId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        ProjectDetailResponse.from(result)));
    }




    @Operation(summary = "프로젝트 목록 조회",
            description = "상태·사업 카테고리·기간·키워드로 필터한 프로젝트를 최근 생성순으로 조회한다. "
                    + "일반 사용자는 참여 중인 프로젝트만, MASTER·ADMIN 은 전 건을 본다 — 권한이 없는 건은 "
                    + "403 이 아니라 목록에서 빠진다. 스텝이 0개면 progressRate 를 담지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "PROJECT_STATUS_INVALID — 허용되지 않은 상태 값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectListResponse>> getProjects(
            @Parameter(description = "프로젝트 상태 필터",
                    schema = @Schema(allowableValues = {
                            "NOT_STARTED", "IN_PROGRESS", "SETTLEMENT", "COMPLETED", "CLOSED"}))
            @RequestParam(required = false) String status,

            @Parameter(description = "사업 카테고리 필터")
            @RequestParam(required = false) Long businessCategoryId,

            @Parameter(description = "시작일 기준 기간 필터 시작 (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedOnFrom,

            @Parameter(description = "시작일 기준 기간 필터 종료 (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedOnTo,

            @Parameter(description = "과업명·발주처 검색어")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "페이지 번호 (0-base)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기 (최대 100)")
            @RequestParam(defaultValue = "20") int size,

            Authentication authentication
    ) {
        ProjectPageResult result = projectQueryUseCase.getProjects(new ProjectListQuery(
                status, businessCategoryId, startedOnFrom, startedOnTo, keyword, page, size,
                authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        ProjectListResponse.from(result)));
    }

    @Operation(summary = "프로젝트 진척률 조회",
            description = "완료 스텝 / 전체 스텝 진척률을 조회한다. 이슈 수는 계산식에 들어가지 않는다. "
                    + "스텝이 0개면 progressRate 를 응답에 담지 않는다 — 0% 로 그리지 마라.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_ACCESS_DENIED — 프로젝트 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 없거나 삭제됨")
    })
    @GetMapping("/{projectId}/progress")
    public ResponseEntity<ApiResponse<ProjectProgressResponse>> getProjectProgress(
            @Parameter(description = "조회할 프로젝트 ID")
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        ProjectProgressResult result = projectQueryUseCase.getProjectProgress(
                new ProjectProgressQuery(projectId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        ProjectProgressResponse.from(result)));
    }
}
package com.group3.vitamins.project.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.application.command.DeleteProjectCommand;
import com.group3.vitamins.project.application.command.UnlinkBusinessCategoryCommand;
import com.group3.vitamins.project.application.query.ProjectDetailQuery;
import com.group3.vitamins.project.application.query.ProjectListQuery;
import com.group3.vitamins.project.application.query.ProjectProgressQuery;
import com.group3.vitamins.project.application.result.ProjectCategoryResult;
import com.group3.vitamins.project.application.result.ProjectDetailResult;
import com.group3.vitamins.project.application.result.ProjectPageResult;
import com.group3.vitamins.project.application.result.ProjectProgressResult;
import com.group3.vitamins.project.application.result.ProjectResult;
import com.group3.vitamins.project.application.usecase.ProjectCommandUseCase;
import com.group3.vitamins.project.application.usecase.ProjectQueryUseCase;
import com.group3.vitamins.project.presentation.api.request.ProjectCategoryLinkRequest;
import com.group3.vitamins.project.presentation.api.request.ProjectCreateRequest;
import com.group3.vitamins.project.presentation.api.response.ProjectCategoryLinkResponse;
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
// ── import 추가
import com.group3.vitamins.project.application.result.ProjectCloseResult;
import com.group3.vitamins.project.application.result.ProjectStatusResult;
import com.group3.vitamins.project.application.result.ProjectUpdateResult;
import com.group3.vitamins.project.presentation.api.request.ProjectCloseRequest;
import com.group3.vitamins.project.presentation.api.request.ProjectStatusUpdateRequest;
import com.group3.vitamins.project.presentation.api.request.ProjectUpdateRequest;
import com.group3.vitamins.project.presentation.api.response.ProjectCloseResponse;
import com.group3.vitamins.project.presentation.api.response.ProjectStatusUpdateResponse;
import com.group3.vitamins.project.presentation.api.response.ProjectUpdateResponse;
import jakarta.validation.Valid;

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
            @Valid @RequestBody ProjectCreateRequest request,
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


    @Operation(summary = "프로젝트 수정",
            description = "과업명·설명·발주처·기간·계약금액을 수정한다. "
                    + "수정 화면의 폼 전체를 보내며, 보내지 않은 필드는 비워진다. "
                    + "상태 변경·종결은 별도 API 다. "
                    + "계약금액은 project.contract_amount 한 곳에만 저장된다 (INV-08).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "PROJECT_NAME_REQUIRED / _NAME_TOO_LONG / _DATE_RANGE_INVALID "
                            + "/ CONTRACT_AMOUNT_INVALID / COMMON_INVALID_REQUEST(형식 오류)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 없거나 삭제됨")
    })
    @PatchMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectUpdateResponse>> updateProject(
            @Parameter(description = "수정할 프로젝트 ID")
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request,
            Authentication authentication
    ) {
        ProjectUpdateResult result = projectCommandUseCase.updateProject(
                request.toCommand(projectId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        ProjectUpdateResponse.from(result)));
    }

    @Operation(summary = "프로젝트 상태 변경",
            description = "NOT_STARTED · IN_PROGRESS · SETTLEMENT · COMPLETED 로 바꾼다. "
                    + "역방향 전이도 막지 않는다 (PRJ-003). "
                    + "CLOSED 는 이 API 로 설정할 수 없고 종결 API 를 쓴다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "PROJECT_STATUS_INVALID — 허용되지 않은 상태 값 (CLOSED 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 없거나 삭제됨")
    })
    @PatchMapping("/{projectId}/status")
    public ResponseEntity<ApiResponse<ProjectStatusUpdateResponse>> changeStatus(
            @Parameter(description = "상태를 바꿀 프로젝트 ID")
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectStatusUpdateRequest request,
            Authentication authentication
    ) {
        ProjectStatusResult result = projectCommandUseCase.changeStatus(
                request.toCommand(projectId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        ProjectStatusUpdateResponse.from(result)));
    }

    @Operation(summary = "프로젝트 종결",
            description = "사유를 붙여 CLOSED 로 만든다. 어느 상태에서든 종결할 수 있다 (PRJ-004). "
                    + "종결해도 목록·활동기록에서 사라지지 않는다 — 삭제와 다른 동작이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "종결 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "CLOSE_REASON_REQUIRED / CLOSE_REASON_INVALID "
                            + "/ CLOSE_REASON_NOTE_TOO_LONG"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 없거나 삭제됨")
    })
    @PostMapping("/{projectId}/close")
    public ResponseEntity<ApiResponse<ProjectCloseResponse>> closeProject(
            @Parameter(description = "종결할 프로젝트 ID")
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectCloseRequest request,
            Authentication authentication
    ) {
        ProjectCloseResult result = projectCommandUseCase.closeProject(
                request.toCommand(projectId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        ProjectCloseResponse.from(result)));
    }

    @Operation(summary = "사업 카테고리 연결",
            description = "프로젝트에 사업 카테고리를 연결한다 (PRJ-007). "
                    + "응답에는 연결 후 전체 카테고리가 담긴다 — 방금 추가한 것만이 아니다. "
                    + "이미 연결된 카테고리가 하나라도 섞이면 409 다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "연결 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "CATEGORY_IDS_REQUIRED — 카테고리 ID 목록이 비어 있음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND / BUSINESS_CATEGORY_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "BUSINESS_CATEGORY_DUPLICATED — 이미 연결된 카테고리")
    })
    @PostMapping("/{projectId}/business-categories")
    public ResponseEntity<ApiResponse<ProjectCategoryLinkResponse>> linkBusinessCategories(
            @Parameter(description = "프로젝트 ID")
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectCategoryLinkRequest request,
            Authentication authentication
    ) {
        ProjectCategoryResult result = projectCommandUseCase.linkBusinessCategories(
                request.toCommand(projectId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        ProjectCategoryLinkResponse.from(result)));
    }

    @Operation(summary = "사업 카테고리 해제",
            description = "프로젝트-카테고리 연결을 끊는다. 연결 행 자체를 지우는 하드 삭제다 — "
                    + "논리 삭제로 두면 UNIQUE 를 시체가 점유해 같은 카테고리를 다시 못 붙인다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND / BUSINESS_CATEGORY_NOT_LINKED")
    })
    @DeleteMapping("/{projectId}/business-categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> unlinkBusinessCategory(
            @Parameter(description = "프로젝트 ID")
            @PathVariable Long projectId,
            @Parameter(description = "해제할 사업 카테고리 ID")
            @PathVariable Long categoryId,
            Authentication authentication
    ) {
        projectCommandUseCase.unlinkBusinessCategory(new UnlinkBusinessCategoryCommand(
                projectId, categoryId, authentication.getName(),
                RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success(ProjectResponseMessage.SUCCESS));
    }

    @Operation(summary = "프로젝트 삭제",
            description = "진행 전이고 스텝이 0개일 때만 논리 삭제한다 (PRJ-014). "
                    + "이미 굴러간 프로젝트는 삭제가 아니라 종결(POST /close)로 남긴다. "
                    + "블록 수는 따로 보지 않는다 — 블록은 스텝에만 붙으므로 스텝이 0개면 블록도 0개다. "
                    + "삭제 시 연결된 공고를 비운다 — 안 그러면 그 공고로 프로젝트를 다시 못 만든다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_EDIT_DENIED — 프로젝트 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 없거나 삭제됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "PROJECT_DELETE_NOT_ALLOWED — 진행 전이 아니거나 스텝이 남아 있음")
    })
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @Parameter(description = "삭제할 프로젝트 ID")
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        projectCommandUseCase.deleteProject(new DeleteProjectCommand(
                projectId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success(ProjectResponseMessage.SUCCESS));
    }
}
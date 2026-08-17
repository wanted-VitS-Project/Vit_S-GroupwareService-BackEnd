package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.usecase.AdminFileTreeUseCase;
import com.group3.vitamins.file.presentation.api.response.AdminTreeProjectPageResponse;
import com.group3.vitamins.file.presentation.api.response.AdminTreeStageListResponse;
import com.group3.vitamins.file.presentation.api.response.AdminTreeStepListResponse;
import com.group3.vitamins.file.presentation.api.response.CompanyFilePageResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전사 파일 트리 탐색(§14 · ADMIN). 프로젝트 → 스테이지 → 스텝 → 파일을 노드별 lazy 조회한다.
 * 검색·필터는 이 트리가 아니라 전사 목록({@code GET /api/v1/admin/files})을 쓴다.
 */
@Tag(name = "File - 전사 파일 트리", description = "전사 파일 탐색기(프로젝트→스테이지→스텝→파일) lazy 조회 · ADMIN 전용 · §14")
@RestController
@RequestMapping("/api/v1/admin/files")
@RequiredArgsConstructor
public class AdminFileTreeController {

    private final AdminFileTreeUseCase adminFileTreeUseCase;

    @Operation(summary = "트리 — 프로젝트 목록(§14.1)",
            description = "회사의 활성 프로젝트를 이름 오름차순으로 페이지 조회한다. ADMIN 전용. 파일 유무로 필터하지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED — ADMIN 권한 없음")
    })
    @GetMapping("/projects")
    public ApiResponse<AdminTreeProjectPageResponse> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return ApiResponse.success(FileResponseMessage.TREE_PROJECTS,
                AdminTreeProjectPageResponse.from(
                        adminFileTreeUseCase.getProjects(RequesterRole.from(authentication), page, size)));
    }

    @Operation(summary = "트리 — 스테이지 목록(§14.2)",
            description = "프로젝트의 활성 스테이지를 sortOrder 오름차순으로 조회한다. stage 미소속 스텝이 있으면 맨 뒤에 "
                    + "미분류 버킷(stageId=null)을 덧붙인다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND — 회사에 없는 프로젝트")
    })
    @GetMapping("/projects/{projectId}/stages")
    public ApiResponse<AdminTreeStageListResponse> getStages(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(FileResponseMessage.TREE_STAGES,
                AdminTreeStageListResponse.from(
                        adminFileTreeUseCase.getStages(RequesterRole.from(authentication), projectId)));
    }

    @Operation(summary = "트리 — 스텝 목록(§14.3)",
            description = "프로젝트의 활성 스텝을 sortOrder 오름차순으로 조회한다. stageId 를 주면 그 스테이지의 스텝, "
                    + "생략하면 미분류(stage 미소속) 스텝을 반환한다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND — 회사에 없는 프로젝트")
    })
    @GetMapping("/projects/{projectId}/steps")
    public ApiResponse<AdminTreeStepListResponse> getSteps(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "스테이지 ID. 생략하면 미분류(stage 미소속) 스텝") @RequestParam(required = false) Long stageId,
            Authentication authentication
    ) {
        return ApiResponse.success(FileResponseMessage.TREE_STEPS,
                AdminTreeStepListResponse.from(
                        adminFileTreeUseCase.getSteps(RequesterRole.from(authentication), projectId, stageId)));
    }

    @Operation(summary = "트리 — 스텝 내 파일(§14.4)",
            description = "스텝의 활성 파일을 문서 단위 최신 완료 버전으로 페이지 조회한다(최신 업로드순). 응답 구조는 전사 파일 목록과 동일. "
                    + "다운로드·미리보기는 클릭 시 §9/§10 을 호출한다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_STEP_NOT_FOUND — 회사에 없는 스텝")
    })
    @GetMapping("/steps/{stepId}/files")
    public ApiResponse<CompanyFilePageResponse> getStepFiles(
            @Parameter(description = "스텝 ID") @PathVariable Long stepId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return ApiResponse.success(FileResponseMessage.TREE_STEP_FILES,
                CompanyFilePageResponse.from(
                        adminFileTreeUseCase.getStepFiles(RequesterRole.from(authentication), stepId, page, size)));
    }
}

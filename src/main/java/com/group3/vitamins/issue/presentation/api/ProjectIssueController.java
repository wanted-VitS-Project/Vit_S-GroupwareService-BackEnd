package com.group3.vitamins.issue.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.issue.application.query.IssueProjectListQuery;
import com.group3.vitamins.issue.application.result.IssueProjectListResult;
import com.group3.vitamins.issue.application.usecase.IssueQueryUseCase;
import com.group3.vitamins.issue.presentation.api.response.ProjectIssueListResponse;
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

@Tag(name = "Issue - 이슈", description = "프로젝트 단위 이슈 API")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/issues")
@RequiredArgsConstructor
public class ProjectIssueController {

    private final IssueQueryUseCase issueQueryUseCase;

    @Operation(
            summary = "프로젝트 단위 이슈 목록 조회",
            description = "프로젝트에 속한 삭제되지 않은 모든 Step의 이슈를 Step별로 묶어 반환한다. "
                    + "이슈가 없는 Step도 빈 배열로 포함한다. Step은 sortOrder 오름차순이다. "
                    + "Step별·프로젝트 전체 이슈 진척도(전체·완료·진행중 수, 완료율)를 함께 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "프로젝트 이슈 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_ACCESS_DENIED — 프로젝트 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트가 존재하지 않음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectIssueListResponse>> getProjectIssues(
            @Parameter(description = "이슈를 조회할 프로젝트 ID")
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        IssueProjectListResult result = issueQueryUseCase.getIssuesByProject(new IssueProjectListQuery(
                projectId,
                authentication.getName(),
                RequesterRole.from(authentication)
        ));

        return ResponseEntity.ok(ApiResponse.success(
                IssueResponseMessage.PROJECT_LIST_SUCCESS,
                ProjectIssueListResponse.from(result)
        ));
    }
}

package com.group3.vitamins.issue.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.issue.application.query.IssueListQuery;
import com.group3.vitamins.issue.application.result.IssueListResult;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.issue.application.usecase.IssueCommandUseCase;
import com.group3.vitamins.issue.application.usecase.IssueQueryUseCase;
import com.group3.vitamins.issue.presentation.api.request.IssueCreateRequest;
import com.group3.vitamins.issue.presentation.api.response.IssueDetailResponse;
import com.group3.vitamins.issue.presentation.api.response.IssueListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Issue - 이슈", description = "Step 이슈 API")
@RestController
@RequestMapping("/api/v1/steps/{stepId}/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueCommandUseCase issueCommandUseCase;
    private final IssueQueryUseCase issueQueryUseCase;

    @Operation(
            summary = "스텝별 이슈 목록 조회",
            description = "현재 Step에 등록된 이슈 목록을 조회한다. blockId가 전달되면 해당 Step에서 특정 Block과 연결된 이슈만 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "이슈 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "ISS_BLOCK_STEP_MISMATCH — Block이 요청한 Step에 속하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_ACCESS_DENIED — 프로젝트 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND / BLOCK_NOT_FOUND")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<IssueListResponse>> getIssues(
            @Parameter(description = "이슈를 조회할 Step ID")
            @PathVariable Long stepId,

            @Parameter(description = "해당 Block과 연결된 이슈만 조회")
            @RequestParam(required = false) Long blockId,

            Authentication authentication
    ) {
        IssueListResult result = issueQueryUseCase.getIssues(new IssueListQuery(
                stepId,
                blockId,
                authentication.getName(),
                RequesterRole.from(authentication)
        ));

        return ResponseEntity.ok(ApiResponse.success(
                IssueResponseMessage.LIST_SUCCESS,
                IssueListResponse.from(result)
        ));
    }

    @Operation(
            summary = "이슈 생성",
            description = "현재 Step에 새로운 이슈를 생성하고 담당자와 관련 Block을 연결한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "ISS_INVALID_REQUEST / ISS_ASSIGNEE_NOT_PROJECT_MEMBER / ISS_BLOCK_STEP_MISMATCH"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ISS_EDIT_PERMISSION_REQUIRED — Step 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "ISS_STEP_NOT_FOUND / ISS_ASSIGNEE_NOT_FOUND / ISS_BLOCK_NOT_FOUND")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<IssueDetailResponse>> createIssue(
            @Parameter(description = "이슈를 생성할 Step 번호")
            @PathVariable Long stepId,
            @RequestBody IssueCreateRequest request,
            Authentication authentication
    ) {
        IssueResult result = issueCommandUseCase.createIssue(
                request.toCommand(stepId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(IssueResponseMessage.CREATE_SUCCESS, IssueDetailResponse.from(result)));
    }
}

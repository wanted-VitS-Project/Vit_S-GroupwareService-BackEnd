package com.group3.vitamins.issue.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.issue.application.usecase.IssueCommandUseCase;
import com.group3.vitamins.issue.presentation.api.request.IssueCreateRequest;
import com.group3.vitamins.issue.presentation.api.response.IssueDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Issue - 이슈", description = "Step 이슈 API")
@RestController
@RequestMapping("/api/v1/steps/{stepId}/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueCommandUseCase issueCommandUseCase;

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

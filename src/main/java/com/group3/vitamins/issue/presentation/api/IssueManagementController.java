package com.group3.vitamins.issue.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.result.IssueStatusResult;
import com.group3.vitamins.issue.application.usecase.IssueCommandUseCase;
import com.group3.vitamins.issue.presentation.api.request.IssueStatusChangeRequest;
import com.group3.vitamins.issue.presentation.api.response.IssueStatusChangeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Issue - 이슈", description = "Issue 단건 API")
@RestController
@RequestMapping("/api/v1/issues")
@RequiredArgsConstructor
public class IssueManagementController {

    private final IssueCommandUseCase issueCommandUseCase;

    @Operation(
            summary = "이슈 상태 변경",
            description = "드래그 앤 드롭 또는 상세 화면의 상태 선택을 통해 이슈 상태를 변경한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "이슈 상태 변경 성공 또는 동일 상태 멱등 처리"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "ISS_STATUS_REQUIRED / ISS_INVALID_STATUS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ISS_EDIT_PERMISSION_REQUIRED — Step 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "ISS_NOT_FOUND — Issue 없음 또는 논리 삭제됨")
    })
    @PatchMapping("/{issueId}/status")
    public ResponseEntity<ApiResponse<IssueStatusChangeResponse>> changeIssueStatus(
            @Parameter(description = "상태를 변경할 이슈 ID")
            @PathVariable Long issueId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
            @RequestBody(required = false) IssueStatusChangeRequest request,
            Authentication authentication
    ) {
        IssueStatusChangeRequest safeRequest = request == null
                ? new IssueStatusChangeRequest(null)
                : request;
        IssueStatusResult result = issueCommandUseCase.changeIssueStatus(
                safeRequest.toCommand(issueId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success(
                IssueResponseMessage.STATUS_CHANGE_SUCCESS,
                IssueStatusChangeResponse.from(result)
        ));
    }

    @Operation(
            summary = "이슈 삭제",
            description = "이슈를 논리 삭제하고, 담당자 및 관련 Block 연결 정보를 함께 제거한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "이슈 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ISS_EDIT_PERMISSION_REQUIRED — Step 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "ISS_NOT_FOUND — Issue 없음 또는 이미 논리 삭제됨")
    })
    @DeleteMapping("/{issueId}")
    public ResponseEntity<ApiResponse<Void>> deleteIssue(
            @Parameter(description = "삭제할 이슈 ID")
            @PathVariable Long issueId,
            Authentication authentication
    ) {
        issueCommandUseCase.deleteIssue(new DeleteIssueCommand(
                issueId,
                authentication.getName(),
                RequesterRole.from(authentication)
        ));

        return ResponseEntity.ok(ApiResponse.success(IssueResponseMessage.DELETE_SUCCESS));
    }
}

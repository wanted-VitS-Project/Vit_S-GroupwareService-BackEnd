package com.group3.vitamins.issue.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.usecase.IssueCommandUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Issue - 이슈", description = "Issue 단건 API")
@RestController
@RequestMapping("/api/v1/issues")
@RequiredArgsConstructor
public class IssueManagementController {

    private final IssueCommandUseCase issueCommandUseCase;

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

package com.group3.vitamins.approval.presentation;

import com.group3.vitamins.approval.application.command.CreateApprovalCommand;
import com.group3.vitamins.approval.application.usecase.ApprovalCommandUseCase;
import com.group3.vitamins.approval.domain.model.ApprovalWithRevision;
import com.group3.vitamins.approval.presentation.api.response.CreateApprovalResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결재 블록 API — `.ai/api/approval.md` (노션 확정).
 *
 * <p>{@code block} 행 생성은 이 컨트롤러의 책임이 아니다(INV-08) — 블록팀이 만든 blockId 에
 * 결재 상세만 붙인다.
 */
@Tag(name = "Approval", description = "결재 API (담당: 이강욱)")
@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalCommandUseCase approvalCommandUseCase;

    @Operation(summary = "결재 블록 생성",
            description = "이미 존재하는 결재 블록(blockId)에 결재 상세(approval + 1회차 approval_revision, 둘 다 DRAFT)를 붙인다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "결재 상세 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "BLOCK_NOT_FOUND — 블록을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "BLOCK_TYPE_MISMATCH — 블록의 type != APPROVAL"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_NOT_PROJECT_MEMBER — 프로젝트 member 아님")
    })
    @PostMapping("/{blockId}/approval")
    public ResponseEntity<ApiResponse<CreateApprovalResponse>> createApproval(
            @Parameter(description = "결재 상세를 붙일 블록 구분 번호", example = "1")
            @PathVariable Long blockId,
            @AuthenticationPrincipal String userId) {

        ApprovalWithRevision result = approvalCommandUseCase.createApproval(
                new CreateApprovalCommand(blockId, userId));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("결재 상세 생성 성공", CreateApprovalResponse.from(result)));
    }
}

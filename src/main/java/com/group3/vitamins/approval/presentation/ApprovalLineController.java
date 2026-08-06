package com.group3.vitamins.approval.presentation;

import com.group3.vitamins.approval.application.command.ApproveApprovalLineCommand;
import com.group3.vitamins.approval.application.command.RejectApprovalLineCommand;
import com.group3.vitamins.approval.application.result.ApprovalLineProcessResult;
import com.group3.vitamins.approval.application.usecase.ApprovalCommandUseCase;
import com.group3.vitamins.approval.presentation.api.request.ApproveApprovalLineRequest;
import com.group3.vitamins.approval.presentation.api.request.RejectApprovalLineRequest;
import com.group3.vitamins.approval.presentation.api.response.ApprovalLineProcessResponse;
import com.group3.vitamins.approval.presentation.api.response.ApprovalLineRejectResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 결재 처리 API(승인·반려) — `.ai/api/approval.md` 11~12번. */
@Tag(name = "Approval", description = "결재 API (담당: 이강욱)")
@RestController
@RequestMapping("/api/v1/approval-lines")
@RequiredArgsConstructor
public class ApprovalLineController {

    private final ApprovalCommandUseCase approvalCommandUseCase;

    @Operation(summary = "결재 승인",
            description = "해당 결재선의 결재자 본인만, ACTIVE 상태일 때만 승인할 수 있다. 다음 결재선이 있으면 ACTIVE로 전환하고 "
                    + "요청 알림을 보내며, 마지막 순번이면 회차·결재 모두 COMPLETED로 종료하고 기안자에게 완료 알림을 보낸다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "승인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_LINE_FORBIDDEN — 해당 결재선의 결재자가 아님(존재하지 않는 lineId 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_LINE_NOT_ACTIVE — 아직 처리할 차례가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_LINE_ALREADY_PROCESSED — 이미 처리된 결재선의 중복 처리 시도")
    })
    @PostMapping("/{lineId}/approve")
    public ApiResponse<ApprovalLineProcessResponse> approve(
            @Parameter(description = "결재선 구분 번호", example = "202")
            @PathVariable Long lineId,
            @Valid @RequestBody ApproveApprovalLineRequest request,
            @AuthenticationPrincipal String userId) {

        ApprovalLineProcessResult result = approvalCommandUseCase.approve(
                new ApproveApprovalLineCommand(lineId, request.opinion(), userId));

        return ApiResponse.success("결재 승인 성공", ApprovalLineProcessResponse.from(result));
    }

    @Operation(summary = "결재 반려",
            description = "해당 결재선의 결재자 본인만, ACTIVE 상태일 때만 반려할 수 있다. 이후 WAITING 단계는 전부 CANCELED, "
                    + "회차·결재 전체는 REJECTED로 종료되며 기안자에게 반려 알림을 보낸다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "반려 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_LINE_FORBIDDEN — 해당 결재선의 결재자가 아님(존재하지 않는 lineId 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_LINE_NOT_ACTIVE — 아직 처리할 차례가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_LINE_ALREADY_PROCESSED — 이미 처리된 결재선의 중복 처리 시도")
    })
    @PostMapping("/{lineId}/reject")
    public ApiResponse<ApprovalLineRejectResponse> reject(
            @Parameter(description = "결재선 구분 번호", example = "201")
            @PathVariable Long lineId,
            @Valid @RequestBody RejectApprovalLineRequest request,
            @AuthenticationPrincipal String userId) {

        ApprovalLineProcessResult result = approvalCommandUseCase.reject(
                new RejectApprovalLineCommand(lineId, request.opinion(), userId));

        return ApiResponse.success("결재 반려 성공", ApprovalLineRejectResponse.from(result));
    }
}

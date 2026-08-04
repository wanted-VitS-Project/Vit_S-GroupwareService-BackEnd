package com.group3.vitamins.approval.presentation;

import com.group3.vitamins.approval.application.command.AddApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.RemoveApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.ResubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.SubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalLinesCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalRevisionCommand;
import com.group3.vitamins.approval.application.query.GetApprovalRevisionQuery;
import com.group3.vitamins.approval.application.result.ApprovalDocumentView;
import com.group3.vitamins.approval.application.result.ApprovalLineView;
import com.group3.vitamins.approval.application.result.ApprovalResubmissionResult;
import com.group3.vitamins.approval.application.result.ApprovalRevisionDetail;
import com.group3.vitamins.approval.application.result.ApprovalSubmissionResult;
import com.group3.vitamins.approval.application.usecase.ApprovalCommandUseCase;
import com.group3.vitamins.approval.application.usecase.ApprovalQueryUseCase;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.presentation.api.request.AddApprovalDocumentRequest;
import com.group3.vitamins.approval.presentation.api.request.UpdateApprovalLinesRequest;
import com.group3.vitamins.approval.presentation.api.request.UpdateApprovalRevisionRequest;
import com.group3.vitamins.approval.presentation.api.response.AddApprovalDocumentResponse;
import com.group3.vitamins.approval.presentation.api.response.ApprovalRevisionDetailResponse;
import com.group3.vitamins.approval.presentation.api.response.ResubmitApprovalRevisionResponse;
import com.group3.vitamins.approval.presentation.api.response.SubmitApprovalResponse;
import com.group3.vitamins.approval.presentation.api.response.UpdateApprovalLinesResponse;
import com.group3.vitamins.approval.presentation.api.response.UpdateApprovalRevisionResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 결재 관리 API — `.ai/api/approval.md` (노션 확정).
 *
 * <p>1번(결재 블록 생성)만 {@code /api/v1/blocks/**} 이고, 나머지(2~8번)는 전부 {@code /api/v1/approvals/**} 다.
 */
@Tag(name = "Approval", description = "결재 API (담당: 이강욱)")
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalRevisionController {

    private final ApprovalCommandUseCase approvalCommandUseCase;
    private final ApprovalQueryUseCase approvalQueryUseCase;

    @Operation(summary = "결재 회차 상세조회",
            description = "기안자·해당 회차 ACTIVE 이상 결재자(과거 이력 포함)·MASTER 만 조회할 수 있다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "APPROVAL_NOT_FOUND / APPROVAL_REVISION_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_LINE_NOT_VIEWABLE — 차례 안 온 결재자(WAITING)의 조회")
    })
    @GetMapping("/{approvalId}/revisions/{revisionId}")
    public ApiResponse<ApprovalRevisionDetailResponse> getRevisionDetail(
            @Parameter(description = "결재 구분 번호", example = "1")
            @PathVariable Long approvalId,
            @Parameter(description = "상신 회차 구분 번호", example = "1")
            @PathVariable Long revisionId,
            @AuthenticationPrincipal String userId) {

        ApprovalRevisionDetail detail = approvalQueryUseCase.getRevisionDetail(
                new GetApprovalRevisionQuery(approvalId, revisionId, userId));

        return ApiResponse.success("조회 성공", ApprovalRevisionDetailResponse.from(detail));
    }

    @Operation(summary = "결재 제목·내용 수정",
            description = "DRAFT 상태의 회차에서 제목·내용을 수정한다. title/content 중 하나만 보내도 부분 수정된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "APPROVAL_NOT_FOUND / APPROVAL_REVISION_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_NOT_DRAFTER — 기안자 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_REVISION_NOT_DRAFT — DRAFT 아닌 회차 수정 시도")
    })
    @PatchMapping("/{approvalId}/revisions/{revisionId}")
    public ApiResponse<UpdateApprovalRevisionResponse> updateRevision(
            @Parameter(description = "결재 구분 번호", example = "1")
            @PathVariable Long approvalId,
            @Parameter(description = "상신 회차 구분 번호", example = "1")
            @PathVariable Long revisionId,
            @Valid @RequestBody UpdateApprovalRevisionRequest request,
            @AuthenticationPrincipal String userId) {

        ApprovalRevision updated = approvalCommandUseCase.updateRevisionDraft(
                new UpdateApprovalRevisionCommand(approvalId, revisionId, request.title(), request.content(), userId));

        return ApiResponse.success("수정 성공", UpdateApprovalRevisionResponse.from(updated));
    }

    @Operation(summary = "결재 문서 추가",
            description = "업로드 완료된 파일 버전을 결재 문서로 연결한다. 실제 파일 업로드는 공용 파일 API 소관이며, "
                    + "이 API는 fileVersionId 연결만 한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "APPROVAL_NOT_FOUND / APPROVAL_REVISION_NOT_FOUND / FILE_VERSION_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_NOT_DRAFTER — 기안자 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_REVISION_NOT_DRAFT / FILE_VERSION_NOT_READY / DOCUMENT_ALREADY_LINKED")
    })
    @PostMapping("/{approvalId}/revisions/{revisionId}/documents")
    public ResponseEntity<ApiResponse<AddApprovalDocumentResponse>> addDocument(
            @Parameter(description = "결재 구분 번호", example = "1")
            @PathVariable Long approvalId,
            @Parameter(description = "상신 회차 구분 번호", example = "1")
            @PathVariable Long revisionId,
            @Valid @RequestBody AddApprovalDocumentRequest request,
            @AuthenticationPrincipal String userId) {

        ApprovalDocumentView view = approvalCommandUseCase.addDocument(
                new AddApprovalDocumentCommand(approvalId, revisionId, request.fileVersionId(), userId));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("추가 성공", AddApprovalDocumentResponse.from(view)));
    }

    @Operation(summary = "결재 문서 제거", description = "DRAFT 상태의 회차에서 연결된 문서를 하드 삭제한다(이력 보존 대상 아님).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "APPROVAL_NOT_FOUND / APPROVAL_REVISION_NOT_FOUND / APPROVAL_DOCUMENT_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_NOT_DRAFTER — 기안자 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_REVISION_NOT_DRAFT")
    })
    @DeleteMapping("/{approvalId}/revisions/{revisionId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> removeDocument(
            @Parameter(description = "결재 구분 번호", example = "1")
            @PathVariable Long approvalId,
            @Parameter(description = "상신 회차 구분 번호", example = "1")
            @PathVariable Long revisionId,
            @Parameter(description = "결재 문서 구분 번호", example = "1")
            @PathVariable Long documentId,
            @AuthenticationPrincipal String userId) {

        approvalCommandUseCase.removeDocument(
                new RemoveApprovalDocumentCommand(approvalId, revisionId, documentId, userId));

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.of(204, "제거 성공", null));
    }

    @Operation(summary = "결재선 등록·수정",
            description = "DRAFT 상태의 회차에서 결재선 전체를 요청받은 목록으로 치환한다. MASTER·ADMIN 은 project member 검증에서 제외된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록·수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "APPROVAL_NOT_FOUND / APPROVAL_REVISION_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_NOT_DRAFTER — 기안자 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_REVISION_NOT_DRAFT — DRAFT 아닌 회차 수정 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "APPROVAL_LINE_EMPTY — 결재자 0명"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "APPROVAL_LINE_ORDER_INVALID — 순서 중복/누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "APPROVAL_LINE_APPROVER_NOT_MEMBER — 일반 결재자가 project member 아님(MASTER·ADMIN 제외)")
    })
    @PutMapping("/{approvalId}/revisions/{revisionId}/lines")
    public ApiResponse<UpdateApprovalLinesResponse> updateLines(
            @Parameter(description = "결재 구분 번호", example = "1")
            @PathVariable Long approvalId,
            @Parameter(description = "상신 회차 구분 번호", example = "1")
            @PathVariable Long revisionId,
            @Valid @RequestBody UpdateApprovalLinesRequest request,
            @AuthenticationPrincipal String userId) {

        List<UpdateApprovalLinesCommand.LineInput> lineInputs = request.lines().stream()
                .map(line -> new UpdateApprovalLinesCommand.LineInput(line.approverId(), line.order()))
                .toList();

        List<ApprovalLineView> result = approvalCommandUseCase.updateLines(
                new UpdateApprovalLinesCommand(approvalId, revisionId, userId, lineInputs));

        return ApiResponse.success("등록·수정 성공", UpdateApprovalLinesResponse.from(result));
    }

    @Operation(summary = "재상신 회차 생성",
            description = "반려된 결재의 새 DRAFT 회차를 만든다. 이전 회차의 제목·내용·문서를 복사하고, "
                    + "결재선은 반려자부터만 재구성한다. 이미 준비된 DRAFT 회차가 있으면 그대로 반환한다(멱등).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "새 회차 생성"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "이미 있는 DRAFT 회차 그대로 반환(멱등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "APPROVAL_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_NOT_DRAFTER — 기안자 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_NOT_REJECTED — approval.status != REJECTED")
    })
    @PostMapping("/{approvalId}/revisions")
    public ResponseEntity<ApiResponse<ResubmitApprovalRevisionResponse>> resubmit(
            @Parameter(description = "결재 구분 번호", example = "1")
            @PathVariable Long approvalId,
            @AuthenticationPrincipal String userId) {

        ApprovalResubmissionResult result = approvalCommandUseCase.resubmit(
                new ResubmitApprovalCommand(approvalId, userId));

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created() ? "새 회차 생성" : "이미 있는 DRAFT 회차 그대로 반환";

        return ResponseEntity.status(status)
                .body(ApiResponse.of(status.value(), message, ResubmitApprovalRevisionResponse.from(result)));
    }

    @Operation(summary = "결재 상신",
            description = "DRAFT 회차를 상신한다. 제목·내용·문서·결재선 유효성을 전부 재검증하고, "
                    + "통과하면 회차·결재는 IN_PROGRESS로, 1번 결재선은 ACTIVE로 전환된다. 최초 상신·재상신 겸용이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "APPROVAL_NOT_FOUND / APPROVAL_REVISION_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "APPROVAL_NOT_DRAFTER — 기안자 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_REVISION_NOT_DRAFT — 이미 상신됐거나 DRAFT 아님(중복 상신 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "APPROVAL_CONTENT_REQUIRED / APPROVAL_DOCUMENT_REQUIRED / "
                            + "APPROVAL_LINE_EMPTY / APPROVAL_LINE_ORDER_INVALID / APPROVAL_LINE_APPROVER_NOT_MEMBER")
    })
    @PostMapping("/{approvalId}/revisions/{revisionId}/submit")
    public ApiResponse<SubmitApprovalResponse> submit(
            @Parameter(description = "결재 구분 번호", example = "1")
            @PathVariable Long approvalId,
            @Parameter(description = "상신 회차 구분 번호", example = "1")
            @PathVariable Long revisionId,
            @AuthenticationPrincipal String userId) {

        ApprovalSubmissionResult result = approvalCommandUseCase.submit(
                new SubmitApprovalCommand(approvalId, revisionId, userId));

        return ApiResponse.success("상신 성공", SubmitApprovalResponse.from(result));
    }
}

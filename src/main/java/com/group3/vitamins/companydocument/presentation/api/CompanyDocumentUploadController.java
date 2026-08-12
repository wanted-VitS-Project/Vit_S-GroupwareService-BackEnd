package com.group3.vitamins.companydocument.presentation.api;

import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentUploadUseCase;
import com.group3.vitamins.companydocument.presentation.api.request.CompanyDocumentUploadCompleteRequest;
import com.group3.vitamins.companydocument.presentation.api.request.StartCompanyDocumentUploadRequest;
import com.group3.vitamins.companydocument.presentation.api.response.CompanyDocumentUploadStartResponse;
import com.group3.vitamins.companydocument.presentation.api.response.CompanyDocumentVersionDetailResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CompanyDocument - 사내 문서함", description = "회사 소속 사내 문서 업로드/버전 — 전사 관리 ADMIN 전용")
@RestController
@RequestMapping("/api/v1/admin/company-documents")
@RequiredArgsConstructor
public class CompanyDocumentUploadController {

    private final CompanyDocumentUploadUseCase companyDocumentUploadUseCase;

    @Operation(summary = "사내 문서 업로드 시작",
            description = "문서/버전 레코드를 UPLOADING 으로 만들고 presigned PUT URL(10분)을 발급한다. "
                    + "companyDocumentId 를 주면 그 문서의 새 버전, 없으면 새 문서(v1, category 필수)다. "
                    + "파일 자체는 응답의 uploadUrl 로 클라이언트가 저장소에 직접 PUT 한 뒤 완료 통보를 호출한다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "발급 성공 — 버전이 UPLOADING 으로 생성됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "CDOC_INVALID_REQUEST · CDOC_SIZE_EXCEEDED · CDOC_EXTENSION_BLOCKED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "CDOC_NOT_FOUND — companyDocumentId 문서 없음(타 회사 포함)")
    })
    @PostMapping("/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyDocumentUploadStartResponse> startUpload(
            @RequestBody StartCompanyDocumentUploadRequest request,
            Authentication authentication
    ) {
        CompanyDocumentUploadStartResponse data = CompanyDocumentUploadStartResponse.from(
                companyDocumentUploadUseCase.startUpload(
                        request.toCommand(authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.created(CompanyDocumentResponseMessage.UPLOAD_STARTED, data);
    }

    @Operation(summary = "사내 문서 업로드 완료 통보",
            description = "클라이언트가 저장소에 PUT 을 마친 뒤 호출한다. 서버가 저장소에 HEAD 로 존재·크기를 검증하고 "
                    + "PDF 면 총 페이지 수를 추출한 뒤 버전을 COMPLETED 로 확정하고 인덱싱 트리거를 발행한다. "
                    + "업로더 정보가 이 시점 스냅샷으로 남는다(ADMIN 은 employee 가 없어 null 일 수 있음). ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "CDOC_ALREADY_COMPLETED — 이미 완료된 버전"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "CDOC_VERSION_NOT_FOUND — 버전 없음(타 회사 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "CDOC_OBJECT_NOT_FOUND(저장소에 객체 없음→FAILED) · CDOC_SIZE_MISMATCH")
    })
    @PostMapping("/uploads/{versionId}/complete")
    public ApiResponse<CompanyDocumentVersionDetailResponse> completeUpload(
            @PathVariable Long versionId,
            @RequestBody(required = false) CompanyDocumentUploadCompleteRequest request,
            Authentication authentication
    ) {
        CompanyDocumentUploadCompleteRequest body =
                request == null ? new CompanyDocumentUploadCompleteRequest(null) : request;
        CompanyDocumentVersionDetailResponse data = CompanyDocumentVersionDetailResponse.from(
                companyDocumentUploadUseCase.completeUpload(
                        body.toCommand(versionId, authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.success(CompanyDocumentResponseMessage.UPLOAD_COMPLETED, data);
    }
}

package com.group3.vitamins.companydocument.presentation.api;

import com.group3.vitamins.companydocument.application.command.DeleteCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.command.RestoreCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentCommandUseCase;
import com.group3.vitamins.companydocument.presentation.api.request.UpdateCompanyDocumentRequest;
import com.group3.vitamins.companydocument.presentation.api.response.CompanyDocumentDeleteResponse;
import com.group3.vitamins.companydocument.presentation.api.response.CompanyDocumentRestoreResponse;
import com.group3.vitamins.companydocument.presentation.api.response.CompanyDocumentUpdateResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CompanyDocument - 사내 문서 관리", description = "사내 문서 수정·삭제·복구 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/admin/company-documents")
@RequiredArgsConstructor
public class CompanyDocumentCommandController {

    private final CompanyDocumentCommandUseCase companyDocumentCommandUseCase;

    @Operation(summary = "사내 문서 표시명·카테고리 수정",
            description = "표시명·카테고리를 수정한다(보낸 것만 반영, 최소 1개 필요). 원본 파일명은 불변. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "CDOC_INVALID_REQUEST — 이름 255자 초과·카테고리 enum 불일치·변경 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CDOC_NOT_FOUND — 문서 없음/이미 삭제")
    })
    @PatchMapping("/{companyDocumentId}")
    public ApiResponse<CompanyDocumentUpdateResponse> update(
            @PathVariable Long companyDocumentId,
            @RequestBody UpdateCompanyDocumentRequest request,
            Authentication authentication
    ) {
        CompanyDocumentUpdateResponse data = CompanyDocumentUpdateResponse.from(
                companyDocumentCommandUseCase.update(
                        request.toCommand(companyDocumentId, authentication.getName(),
                                RequesterRole.from(authentication))));

        return ApiResponse.success(CompanyDocumentResponseMessage.DOCUMENT_UPDATED, data);
    }

    @Operation(summary = "사내 문서 삭제",
            description = "soft delete 로 삭제한다(저장소 객체 유지, 복구 가능). 인덱스에서 제외된다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CDOC_ALREADY_DELETED — 이미 삭제"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CDOC_NOT_FOUND — 문서 없음")
    })
    @DeleteMapping("/{companyDocumentId}")
    public ApiResponse<CompanyDocumentDeleteResponse> delete(
            @PathVariable Long companyDocumentId,
            Authentication authentication
    ) {
        CompanyDocumentDeleteResponse data = CompanyDocumentDeleteResponse.from(
                companyDocumentCommandUseCase.delete(new DeleteCompanyDocumentCommand(
                        companyDocumentId, authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.success(CompanyDocumentResponseMessage.DOCUMENT_DELETED, data);
    }

    @Operation(summary = "사내 문서 복구",
            description = "삭제된 문서를 복구한다. 인덱스에 재등록된다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "복구 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CDOC_NOT_DELETED — 삭제 상태 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CDOC_NOT_FOUND — 문서 없음")
    })
    @PostMapping("/{companyDocumentId}/restore")
    public ApiResponse<CompanyDocumentRestoreResponse> restore(
            @PathVariable Long companyDocumentId,
            Authentication authentication
    ) {
        CompanyDocumentRestoreResponse data = CompanyDocumentRestoreResponse.from(
                companyDocumentCommandUseCase.restore(new RestoreCompanyDocumentCommand(
                        companyDocumentId, authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.success(CompanyDocumentResponseMessage.DOCUMENT_RESTORED, data);
    }
}

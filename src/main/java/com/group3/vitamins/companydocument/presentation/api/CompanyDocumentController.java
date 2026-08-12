package com.group3.vitamins.companydocument.presentation.api;

import com.group3.vitamins.companydocument.application.query.CompanyDocumentListQuery;
import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentQueryUseCase;
import com.group3.vitamins.companydocument.presentation.api.response.CompanyDocumentPageResponse;
import com.group3.vitamins.companydocument.presentation.api.response.CompanyDocumentVersionHistoryResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CompanyDocument - 사내 문서함 조회", description = "사내 문서 목록·버전 이력 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/admin/company-documents")
@RequiredArgsConstructor
public class CompanyDocumentController {

    private final CompanyDocumentQueryUseCase companyDocumentQueryUseCase;

    @Operation(summary = "사내 문서 목록",
            description = "회사(테넌트) 안의 사내 문서를 문서 단위 최신 완료 버전으로 페이지 조회한다. ADMIN 전용. "
                    + "카테고리·검색어(표시명/원본명)로 필터한다. 완료 버전이 없는 문서는 제외된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED — ADMIN 아님")
    })
    @GetMapping
    public ApiResponse<CompanyDocumentPageResponse> getDocuments(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        CompanyDocumentPageResponse data = CompanyDocumentPageResponse.from(
                companyDocumentQueryUseCase.getDocuments(new CompanyDocumentListQuery(
                        authentication.getName(), RequesterRole.from(authentication),
                        category, keyword, page, size)));

        return ApiResponse.success(CompanyDocumentResponseMessage.DOCUMENT_LIST, data);
    }

    @Operation(summary = "사내 문서 버전 이력",
            description = "문서의 완료 버전을 차수 내림차순으로 돌려준다(append-only, 조회 전용). ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CDOC_NOT_FOUND — 문서 없음")
    })
    @GetMapping("/{companyDocumentId}/versions")
    public ApiResponse<CompanyDocumentVersionHistoryResponse> getVersionHistory(
            @PathVariable Long companyDocumentId,
            Authentication authentication
    ) {
        CompanyDocumentVersionHistoryResponse data = CompanyDocumentVersionHistoryResponse.from(
                companyDocumentQueryUseCase.getVersionHistory(
                        companyDocumentId, authentication.getName(), RequesterRole.from(authentication)));

        return ApiResponse.success(CompanyDocumentResponseMessage.VERSION_HISTORY, data);
    }
}

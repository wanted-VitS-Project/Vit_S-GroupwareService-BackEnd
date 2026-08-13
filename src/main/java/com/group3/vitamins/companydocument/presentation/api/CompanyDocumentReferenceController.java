package com.group3.vitamins.companydocument.presentation.api;

import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentReferenceUseCase;
import com.group3.vitamins.companydocument.presentation.api.response.CompanyDocumentReferenceResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 사내 문서 <b>참조 선택</b> API (입찰 검토 비교자료 선택 · COMPANY-DOC-V1 §2-G 연동).
 *
 * <p>관리(ADMIN) API(`/api/v1/admin/company-documents`)와 달리 <b>회사 소속이면 누구나</b> 호출한다(인증만).
 * 회사 스코프·완료 최신 버전만 노출하고, 참조는 버전 고정으로 준다.
 */
@Tag(name = "CompanyDocument - 참조 선택", description = "입찰 검토용 사내 문서 참조 선택 (회사 소속 누구나) — 담당: 김동현")
@RestController
@RequestMapping("/api/v1/company-documents")
@RequiredArgsConstructor
public class CompanyDocumentReferenceController {

    private final CompanyDocumentReferenceUseCase referenceUseCase;

    @Operation(summary = "참조 선택용 사내 문서 목록",
            description = "입찰 검토에서 비교 자료로 고를 사내 문서를 조회한다. 회사 스코프·완료(COMPLETED) 최신 버전만 내려가며, "
                    + "참조는 companyDocumentVersionId(버전 고정)로 잡는다. category·keyword 로 필터한다. "
                    + "인덱스 준비 상태(indexStatus) 노출·필터는 §6-2(AI 인덱싱) 확정 후 붙는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @GetMapping("/selectable")
    public ApiResponse<List<CompanyDocumentReferenceResponse>> listSelectable(
            @Parameter(description = "분류 필터 (FINANCE·COMPANY_INTRO·PERFORMANCE·CERTIFICATE·ETC)")
            @RequestParam(required = false) String category,
            @Parameter(description = "이름·파일명 부분 검색")
            @RequestParam(required = false) String keyword) {

        List<CompanyDocumentReferenceResponse> data = referenceUseCase.listSelectable(category, keyword).stream()
                .map(CompanyDocumentReferenceResponse::from)
                .toList();

        return ApiResponse.success(CompanyDocumentResponseMessage.SELECTABLE_LIST, data);
    }
}

package com.group3.vitamins.finance.presentation.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.finance.application.query.CashFlowFilterQuery;
import com.group3.vitamins.finance.application.query.CashFlowListQuery;
import com.group3.vitamins.finance.application.query.FinanceSummaryQuery;
import com.group3.vitamins.finance.application.query.MatchCandidatesQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceFilterQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceListQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceMatchCandidatesQuery;
import com.group3.vitamins.finance.application.command.CashFlowCsvPreviewCommand;
import com.group3.vitamins.finance.application.command.CreateCashFlowCommand;
import com.group3.vitamins.finance.application.command.DeleteCashFlowsCommand;
import com.group3.vitamins.finance.application.command.MatchCashFlowCommand;
import com.group3.vitamins.finance.application.command.MatchTaxInvoiceCommand;
import com.group3.vitamins.finance.application.command.TaxInvoiceCsvPreviewCommand;
import com.group3.vitamins.finance.application.command.UnmatchCashFlowCommand;
import com.group3.vitamins.finance.application.command.UnmatchTaxInvoiceCommand;
import com.group3.vitamins.finance.application.command.UpdateTaxInvoiceMemoCommand;
import com.group3.vitamins.finance.application.command.DeleteTaxInvoicesCommand;
import com.group3.vitamins.finance.application.command.UpdateTaxInvoiceExclusionCommand;
import com.group3.vitamins.finance.application.command.UpdateCashFlowExclusionCommand;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowCsvPreviewView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowCsvUploadView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowDeleteResultView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowDetailView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowExclusionResultView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowMatchView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceCsvPreviewView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceCsvUploadView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceMatchView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceMemoView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceDeleteResultView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceExclusionResultView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.CashFlowFilterView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.CashFlowListView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.FinanceSummaryView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.MatchCandidatesView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.TaxInvoiceFilterView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.TaxInvoiceListView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.TaxInvoiceMatchCandidatesView;
import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.finance.presentation.api.request.CashFlowCsvUploadRequest;
import com.group3.vitamins.finance.presentation.api.response.CashFlowCsvPreviewResponse;
import com.group3.vitamins.finance.presentation.api.response.CashFlowCsvUploadResponse;
import com.group3.vitamins.finance.presentation.api.response.CashFlowFilterResponse;
import com.group3.vitamins.finance.presentation.api.request.CreateCashFlowRequest;
import com.group3.vitamins.finance.presentation.api.request.DeleteCashFlowsRequest;
import com.group3.vitamins.finance.presentation.api.request.UpdateCashFlowExclusionRequest;
import com.group3.vitamins.finance.presentation.api.request.UpdateCashFlowRequest;
import com.group3.vitamins.finance.presentation.api.response.CashFlowCreateResponse;
import com.group3.vitamins.finance.presentation.api.response.CashFlowDeleteResponse;
import com.group3.vitamins.finance.presentation.api.response.CashFlowExclusionResponse;
import com.group3.vitamins.finance.presentation.api.response.CashFlowListResponse;
import com.group3.vitamins.finance.presentation.api.response.CashFlowMatchCandidatesResponse;
import com.group3.vitamins.finance.presentation.api.response.CashFlowMatchResponse;
import com.group3.vitamins.finance.presentation.api.response.CashFlowUpdateResponse;
import com.group3.vitamins.finance.presentation.api.request.MatchCashFlowRequest;
import com.group3.vitamins.finance.presentation.api.request.MatchTaxInvoiceRequest;
import com.group3.vitamins.finance.presentation.api.response.FinanceSummaryResponse;
import com.group3.vitamins.finance.presentation.api.request.TaxInvoiceCsvUploadRequest;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceCsvPreviewResponse;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceCsvUploadResponse;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceFilterResponse;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceListResponse;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceMatchCandidatesResponse;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceMatchResponse;
import com.group3.vitamins.finance.presentation.api.request.UpdateTaxInvoiceMemoRequest;
import com.group3.vitamins.finance.presentation.api.request.UpdateTaxInvoiceExclusionRequest;
import com.group3.vitamins.finance.presentation.api.request.DeleteTaxInvoicesRequest;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceMemoResponse;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceDeleteResponse;
import com.group3.vitamins.finance.presentation.api.response.TaxInvoiceExclusionResponse;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Tag(name = "Finance", description = "재무 관리 API")
@RestController
@RequestMapping("api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceQueryUseCase financeQueryUseCase;
    private final FinanceCommandUseCase financeCommandUseCase;
    private final ObjectMapper objectMapper;

    @Operation(summary = "재무 관리 요약 조회",
            description = "재무 관리 페이지 진입 시 입출금 내역·세금계산서·정산 현황을 한 화면에 요약해서 보여준다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재무 관리 요약 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "접근 권한이 없습니다. (FINANCE_ACCESS_DENIED)")
    })
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<FinanceSummaryResponse>> getSummary(Authentication authentication) {
        FinanceSummaryView view = financeQueryUseCase.getSummary(
                new FinanceSummaryQuery(authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("재무 관리 요약 조회 성공", FinanceSummaryResponse.from(view)));
    }

    @Operation(summary = "입출금 내역 조회",
            description = "재무 관리 페이지의 입출금 내역 목록을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입출금 내역 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "접근 권한이 없습니다. (FINANCE_ACCESS_DENIED)")
    })
    @GetMapping("/cash-flows")
    public ResponseEntity<ApiResponse<CashFlowListResponse>> getCashFlows(
            @Parameter(description = "조회 시작일(tradedAt 날짜 기준)", example = "2026-07-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일(tradedAt 날짜 기준)", example = "2026-07-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "미연결 항목만 조회(true: 미매칭, 없으면 전체)", example = "true")
            @RequestParam(required = false) Boolean unlinked,
            @Parameter(description = "매칭 프로젝트 필터", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "적요 또는 입금자명 검색 키워드", example = "환경부")
            @RequestParam(required = false) String keyword,
            Authentication authentication
    ) {
        CashFlowListView view = financeQueryUseCase.getCashFlows(new CashFlowListQuery(
                startDate, endDate, unlinked, projectId, keyword,
                authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("입출금 내역 조회 성공", CashFlowListResponse.from(view)));
    }

    @Operation(summary = "입출금 내역 필터 옵션 조회",
            description = "입출금 내역 조회 화면의 프로젝트 필터 옵션을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입출금 내역 필터 옵션 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "접근 권한이 없습니다. (FINANCE_ACCESS_DENIED)")
    })
    @GetMapping("/cash-flows/filters")
    public ResponseEntity<ApiResponse<CashFlowFilterResponse>> getCashFlowFilters(Authentication authentication) {
        CashFlowFilterView view = financeQueryUseCase.getCashFlowFilters(
                new CashFlowFilterQuery(authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("입출금 내역 필터 옵션 조회 성공", CashFlowFilterResponse.from(view)));
    }

    @Operation(summary = "세금계산서 조회",
            description = "재무 관리 페이지의 세금계산서 목록을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세금계산서 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "페이지 조회 조건이 올바르지 않습니다. (FINANCE_PAGE_QUERY_INVALID)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "접근 권한이 없습니다. (FINANCE_ACCESS_DENIED)")
    })
    @GetMapping("/tax-invoices")
    public ResponseEntity<ApiResponse<TaxInvoiceListResponse>> getTaxInvoices(
            @Parameter(description = "조회 시작일(issuedNo 날짜 기준)", example = "2026-07-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일(issuedNo 날짜 기준)", example = "2026-07-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "미연결 항목만 조회(true: 미매칭, 없으면 전체)", example = "true")
            @RequestParam(required = false) Boolean unlinked,
            @Parameter(description = "매칭 프로젝트 필터", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "승인번호 또는 공급받는자 상호명 검색 키워드", example = "환경부")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "0-base 페이지 번호. 생략하면 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 개수(최대 100). 생략하면 20", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준. 생략하면 ISSUED_NO_DESC",
                    example = "ISSUED_NO_DESC",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"ISSUED_NO_DESC", "ISSUED_NO_ASC", "AMOUNT_DESC"}))
            @RequestParam(defaultValue = "ISSUED_NO_DESC") String sort,
            Authentication authentication
    ) {
        TaxInvoiceListView view = financeQueryUseCase.getTaxInvoices(new TaxInvoiceListQuery(
                startDate, endDate, unlinked, projectId, keyword, page, size, sort,
                authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("세금계산서 조회 성공", TaxInvoiceListResponse.from(view)));
    }

    @Operation(summary = "세금계산서 필터 옵션 조회",
            description = "세금계산서 조회 화면의 프로젝트 필터 옵션을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세금계산서 필터 옵션 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "접근 권한이 없습니다. (FINANCE_ACCESS_DENIED)")
    })
    @GetMapping("/tax-invoices/filters")
    public ResponseEntity<ApiResponse<TaxInvoiceFilterResponse>> getTaxInvoiceFilters(Authentication authentication) {
        TaxInvoiceFilterView view = financeQueryUseCase.getTaxInvoiceFilters(
                new TaxInvoiceFilterQuery(authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("세금계산서 필터 옵션 조회 성공", TaxInvoiceFilterResponse.from(view)));
    }

    @Operation(summary = "세금계산서 CSV 컬럼 추천 조회",
            description = "업로드한 CSV(또는 엑셀 .xlsx/.xls)의 컬럼 목록·미리보기·추천 컬럼 매핑 + 추천 구분(INCOME/OUTCOME)을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세금계산서 CSV 컬럼 추천 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "비밀번호가 필요한 파일입니다. (FINANCE_CSV_PASSWORD_REQUIRED) / "
                            + "비밀번호가 올바르지 않습니다. (FINANCE_CSV_PASSWORD_INVALID)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "유효하지 않은 형식입니다. (FINANCE_INVALID_CSV_FILE)")
    })
    @PostMapping(value = "/tax-invoices/csv/preview", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<TaxInvoiceCsvPreviewResponse>> previewTaxInvoiceCsv(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(description = "파일이 비밀번호로 보호돼 있으면 그 비밀번호(엑셀만 해당). "
                    + "원 명세엔 없던 필드 — cash_flow와 동일하게 추가함(2026-08-12)")
            @RequestPart(value = "password", required = false) String password,
            Authentication authentication
    ) {
        byte[] fileBytes = readBytes(file);
        TaxInvoiceCsvPreviewView view = financeCommandUseCase.previewTaxInvoiceCsv(
                new TaxInvoiceCsvPreviewCommand(fileBytes, file.getOriginalFilename(), password,
                        authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("세금계산서 CSV 컬럼 추천 조회 성공", TaxInvoiceCsvPreviewResponse.from(view)));
    }

    @Operation(summary = "세금계산서(CSV 기반) 업로드",
            description = "미리보기에서 확정한 구분·컬럼 매핑으로 CSV(또는 엑셀 .xlsx/.xls)를 파싱해 세금계산서로 저장한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "세금계산서(CSV 기반) 업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "필수 컬럼 매핑이 누락되었습니다. (FINANCE_CSV_MAPPING_REQUIRED) / "
                            + "비밀번호가 필요한 파일입니다. (FINANCE_CSV_PASSWORD_REQUIRED) / "
                            + "비밀번호가 올바르지 않습니다. (FINANCE_CSV_PASSWORD_INVALID)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)")
    })
    @PostMapping(value = "/tax-invoices/csv", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<TaxInvoiceCsvUploadResponse>> uploadTaxInvoiceCsv(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(description = "구분 + 컬럼 매핑 정보 JSON 문자열. password는 파일이 비밀번호로 "
                    + "보호된 경우에만 필요(엑셀만 해당, 선택 필드라 없으면 생략 가능)",
                    example = "{\"type\": \"INCOME\", \"approvalNoColumn\": \"승인번호\", "
                            + "\"issuedDateColumn\": \"작성일자\", \"supplierBizNoColumn\": \"공급자사업자번호\", "
                            + "\"buyerBizNoColumn\": \"공급받는자사업자번호\", \"buyerNameColumn\": \"상호\", "
                            + "\"supplyAmountColumn\": \"공급가액\", \"taxAmountColumn\": \"세액\", "
                            + "\"totalAmountColumn\": \"합계금액\", \"itemNameColumn\": \"품목\", "
                            + "\"ceoNameColumn\": null, \"subBizNoColumn\": null, \"memoColumn\": null, "
                            + "\"password\": null}")
            @RequestPart(value = "request", required = false) String requestJson,
            Authentication authentication
    ) {
        TaxInvoiceCsvUploadRequest request = parseTaxInvoiceUploadRequest(requestJson);

        byte[] fileBytes = readBytes(file);
        TaxInvoiceCsvUploadView view = financeCommandUseCase.uploadTaxInvoiceCsv(
                request.toCommand(fileBytes, file.getOriginalFilename(),
                        authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("세금계산서(CSV 기반) 업로드 성공", TaxInvoiceCsvUploadResponse.from(view)));
    }

    @Operation(summary = "세금계산서 매칭 추천 조회",
            description = "세금계산서의 발행일·금액·세액·공급받는자 상호명을 기준으로 매칭할 만한 정산 블록 후보를 추천한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매칭 추천 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 세금계산서입니다. (FINANCE_TAX_INVOICE_NOT_FOUND)")
    })
    @GetMapping("/tax-invoices/{taxId}/match-candidates")
    public ResponseEntity<ApiResponse<TaxInvoiceMatchCandidatesResponse>> getTaxInvoiceMatchCandidates(
            @Parameter(description = "매칭할 세금계산서 ID", example = "1") @PathVariable Long taxId,
            Authentication authentication
    ) {
        TaxInvoiceMatchCandidatesView view = financeQueryUseCase.getTaxInvoiceMatchCandidates(
                new TaxInvoiceMatchCandidatesQuery(taxId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("매칭 추천 조회 성공", TaxInvoiceMatchCandidatesResponse.from(view)));
    }

    @Operation(summary = "세금계산서 블록 매칭",
            description = "세금계산서를 정산 블록에 연결한다. 정산 블록당 매칭은 1번뿐이다 — 이미 매칭된 정산 블록에는 다시 매칭할 수 없다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세금계산서 블록 매칭 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "이미 매칭된 항목입니다. (FINANCE_TAX_INVOICE_ALREADY_MATCHED) / "
                            + "세금계산서 구분과 정산 블록 타입이 일치하지 않습니다. (FINANCE_TAX_TYPE_MISMATCH) / "
                            + "이미 매칭된 정산 블록입니다. (FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 세금계산서 또는 정산 블록입니다. (FINANCE_TAX_MATCH_TARGET_NOT_FOUND)")
    })
    @PatchMapping("/tax-invoices/{taxId}/match")
    public ResponseEntity<ApiResponse<TaxInvoiceMatchResponse>> matchTaxInvoice(
            @Parameter(description = "매칭할 세금계산서 ID", example = "1") @PathVariable Long taxId,
            @RequestBody MatchTaxInvoiceRequest request,
            Authentication authentication
    ) {
        TaxInvoiceMatchView view = financeCommandUseCase.matchTaxInvoice(new MatchTaxInvoiceCommand(
                taxId, request.settleId(), authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("세금계산서 블록 매칭 성공", TaxInvoiceMatchResponse.from(view)));
    }

    @Operation(summary = "세금계산서 블록 매칭 해제",
            description = "세금계산서와 정산 블록의 연결을 해제하고, 그 정산 블록을 미연결(PENDING) 상태로 되돌린다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세금계산서 블록 매칭 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "매칭되지 않은 항목입니다. (FINANCE_TAX_INVOICE_NOT_MATCHED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 세금계산서입니다. (FINANCE_TAX_INVOICE_NOT_FOUND)")
    })
    @PatchMapping("/tax-invoices/{taxId}/unmatch")
    public ResponseEntity<ApiResponse<Void>> unmatchTaxInvoice(
            @Parameter(description = "매칭 해제할 세금계산서 ID", example = "1") @PathVariable Long taxId,
            Authentication authentication
    ) {
        financeCommandUseCase.unmatchTaxInvoice(
                new UnmatchTaxInvoiceCommand(taxId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("세금계산서 블록 매칭 해제 성공", null));
    }

    @Operation(summary = "세금계산서 메모 수정",
            description = "세금계산서의 비고/메모를 수정한다. 세금계산서는 수동 등록이 없어(전부 CSV/엑셀 업로드) "
                    + "메모만 수정할 수 있다 — 승인번호·금액·사업자번호는 국세청 발급 원본 값이라 고칠 수 없다. "
                    + "매칭된 항목의 메모도 수정할 수 있다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세금계산서 메모 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 세금계산서입니다. (FINANCE_TAX_INVOICE_NOT_FOUND)")
    })
    @PatchMapping("/tax-invoices/{taxId}")
    public ResponseEntity<ApiResponse<TaxInvoiceMemoResponse>> updateTaxInvoiceMemo(
            @Parameter(description = "수정할 세금계산서 ID", example = "1") @PathVariable Long taxId,
            @RequestBody UpdateTaxInvoiceMemoRequest request,
            Authentication authentication
    ) {
        TaxInvoiceMemoView view = financeCommandUseCase.updateTaxInvoiceMemo(new UpdateTaxInvoiceMemoCommand(
                taxId, request.memo(), authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("세금계산서 메모 수정 성공", TaxInvoiceMemoResponse.from(view)));
    }

    @Operation(summary = "세금계산서 삭제",
            description = "세금계산서 여러 건을 한 번에 삭제한다(소프트 삭제). 정산 블록에 매칭된 항목은 먼저 매칭을 "
                    + "해제해야 삭제할 수 있다 — 매칭됐거나 존재하지 않는 항목은 전체를 실패시키지 않고 "
                    + "skippedItems로 사유와 함께 돌려준다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세금계산서 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "삭제할 항목을 선택해주세요. (FINANCE_TAX_INVOICE_REQUIRED_FIELD_MISSING)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)")
    })
    @DeleteMapping("/tax-invoices")
    public ResponseEntity<ApiResponse<TaxInvoiceDeleteResponse>> deleteTaxInvoices(
            @RequestBody DeleteTaxInvoicesRequest request,
            Authentication authentication
    ) {
        TaxInvoiceDeleteResultView view = financeCommandUseCase.deleteTaxInvoices(new DeleteTaxInvoicesCommand(
                request.taxIds(), authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("세금계산서 삭제 성공", TaxInvoiceDeleteResponse.from(view)));
    }

    @Operation(summary = "세금계산서 연결 제외/포함 처리",
            description = "프로젝트와 무관한 세금계산서를 미연결 건수 집계에서 빼거나(제외), 다시 포함시킨다. "
                    + "이미 매칭된 항목은 제외 처리할 수 없다(제외 취소는 매칭 여부와 무관하게 항상 가능). "
                    + "처리하지 못한 항목은 전체를 실패시키지 않고 skippedItems로 사유와 함께 돌려준다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세금계산서 연결 제외 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "필수 항목이 누락되었습니다. (FINANCE_TAX_INVOICE_REQUIRED_FIELD_MISSING)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)")
    })
    @PatchMapping("/tax-invoices/exclude")
    public ResponseEntity<ApiResponse<TaxInvoiceExclusionResponse>> updateTaxInvoiceExclusion(
            @RequestBody UpdateTaxInvoiceExclusionRequest request,
            Authentication authentication
    ) {
        TaxInvoiceExclusionResultView view = financeCommandUseCase.updateTaxInvoiceExclusion(
                new UpdateTaxInvoiceExclusionCommand(request.taxIds(), request.isExcluded(),
                        authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success("세금계산서 연결 제외 처리 성공", TaxInvoiceExclusionResponse.from(view)));
    }

    private TaxInvoiceCsvUploadRequest parseTaxInvoiceUploadRequest(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, "request가 필요합니다.");
        }
        try {
            return objectMapper.readValue(requestJson, TaxInvoiceCsvUploadRequest.class);
        } catch (JsonProcessingException e) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, e);
        }
    }

    @Operation(summary = "입출금 내역 CSV 컬럼 추천 조회",
            description = "업로드한 CSV(또는 엑셀 .xlsx/.xls)의 컬럼 목록·미리보기·추천 컬럼 매핑을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV 컬럼 추천 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "비밀번호가 필요한 파일입니다. (FINANCE_CSV_PASSWORD_REQUIRED) / "
                            + "비밀번호가 올바르지 않습니다. (FINANCE_CSV_PASSWORD_INVALID)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "유효하지 않은 형식입니다. (FINANCE_INVALID_CSV_FILE)")
    })
    @PostMapping(value = "/cash-flows/csv/preview", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<CashFlowCsvPreviewResponse>> previewCashFlowCsv(
            // required=false라야 파일 part 누락 시 Spring이 컨트롤러 前에 튕기지 않고 서비스가
            // FINANCE_INVALID_CSV_FILE로 판정한다(EmployeeBulkController 선례와 동일).
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(description = "파일이 비밀번호로 보호돼 있으면 그 비밀번호(엑셀만 해당). "
                    + "원 명세엔 없던 필드 — 실제 은행 엑셀 내보내기가 비밀번호로 잠겨오는 경우가 흔해 추가함(2026-08-10)")
            @RequestPart(value = "password", required = false) String password,
            Authentication authentication
    ) {
        // readBytes가 file==null이면 먼저 던지므로, 아래 getOriginalFilename() 시점엔 file이 non-null이다.
        byte[] fileBytes = readBytes(file);
        CashFlowCsvPreviewView view = financeCommandUseCase.previewCashFlowCsv(
                new CashFlowCsvPreviewCommand(fileBytes, file.getOriginalFilename(), password,
                        authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("CSV 컬럼 추천 조회 성공", CashFlowCsvPreviewResponse.from(view)));
    }

    @Operation(summary = "입출금 내역(CSV 기반) 업로드",
            description = "미리보기에서 확정한 컬럼 매핑으로 CSV(또는 엑셀 .xlsx/.xls)를 파싱해 입출금 내역으로 저장한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "입출금 내역(CSV 기반) 업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "필수 컬럼 매핑이 누락되었습니다. (FINANCE_CSV_MAPPING_REQUIRED) / "
                            + "비밀번호가 필요한 파일입니다. (FINANCE_CSV_PASSWORD_REQUIRED) / "
                            + "비밀번호가 올바르지 않습니다. (FINANCE_CSV_PASSWORD_INVALID)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)")
    })
    @PostMapping(value = "/cash-flows/csv", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<CashFlowCsvUploadResponse>> uploadCashFlowCsv(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(description = "은행명 + 컬럼 매핑 정보 JSON 문자열. password는 파일이 비밀번호로 "
                    + "보호된 경우에만 필요(엑셀만 해당, 선택 필드라 없으면 생략 가능)",
                    example = "{\"bankName\": \"신한은행\", \"dateTimeMode\": \"SINGLE\", "
                            + "\"tradedDateTimeColumn\": \"날짜\", \"amountMode\": \"SEPARATE\", "
                            + "\"incomeAmountColumn\": \"입금금액\", \"outcomeAmountColumn\": \"출금금액\", "
                            + "\"memoColumn\": \"적요\", \"depositorColumn\": \"내용\", \"password\": null}")
            @RequestPart(value = "request", required = false) String requestJson,
            Authentication authentication
    ) {
        // request 파트를 @RequestPart(CashFlowCsvUploadRequest)로 바로 받으면, 클라이언트가 이 파트에
        // Content-Type을 안 붙였을 때(Swagger UI 등) Spring이 application/octet-stream으로 간주해
        // 415로 거부한다 — image 도메인 선례와 동일하게 문자열로 받아 직접 파싱한다.
        CashFlowCsvUploadRequest request = parseUploadRequest(requestJson);

        byte[] fileBytes = readBytes(file);
        CashFlowCsvUploadView view = financeCommandUseCase.uploadCashFlowCsv(
                request.toCommand(fileBytes, file.getOriginalFilename(),
                        authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("입출금 내역(CSV 기반) 업로드 성공", CashFlowCsvUploadResponse.from(view)));
    }

    @Operation(summary = "입출금 내역 매칭 추천 조회",
            description = "입출금 내역의 일자·금액·거래처명을 기준으로 매칭할 만한 정산 블록 후보를 추천한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매칭 추천 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 입출금 내역입니다. (FINANCE_CASH_FLOW_NOT_FOUND)")
    })
    @GetMapping("/cash-flows/{cashFlowId}/match-candidates")
    public ResponseEntity<ApiResponse<CashFlowMatchCandidatesResponse>> getMatchCandidates(
            @Parameter(description = "매칭할 입출금 내역 ID", example = "1") @PathVariable Long cashFlowId,
            Authentication authentication
    ) {
        MatchCandidatesView view = financeQueryUseCase.getMatchCandidates(
                new MatchCandidatesQuery(cashFlowId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("매칭 추천 조회 성공", CashFlowMatchCandidatesResponse.from(view)));
    }

    @Operation(summary = "입출금 내역 블록 매칭",
            description = "입출금 내역을 정산 블록에 연결한다. 정산 블록당 매칭은 1번뿐이다 — 이미 매칭된 정산 블록에는 다시 매칭할 수 없다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입출금 내역 블록 매칭 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "이미 매칭된 항목입니다. (FINANCE_CASH_FLOW_ALREADY_MATCHED) / "
                            + "입출금 구분과 정산 블록 타입이 일치하지 않습니다. (FINANCE_MATCH_TYPE_MISMATCH) / "
                            + "이미 매칭된 정산 블록입니다. (FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 입출금 내역 또는 정산 블록입니다. (FINANCE_MATCH_TARGET_NOT_FOUND)")
    })
    @PatchMapping("/cash-flows/{cashFlowId}/match")
    public ResponseEntity<ApiResponse<CashFlowMatchResponse>> matchCashFlow(
            @Parameter(description = "매칭할 입출금 내역 ID", example = "5") @PathVariable Long cashFlowId,
            @RequestBody MatchCashFlowRequest request,
            Authentication authentication
    ) {
        CashFlowMatchView view = financeCommandUseCase.matchCashFlow(new MatchCashFlowCommand(
                cashFlowId, request.settleId(), authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("입출금 내역 블록 매칭 성공", CashFlowMatchResponse.from(view)));
    }

    @Operation(summary = "입출금 내역 블록 매칭 해제",
            description = "입출금 내역과 정산 블록의 연결을 해제하고, 그 정산 블록을 미연결(PENDING) 상태로 되돌린다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입출금 내역 블록 매칭 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "매칭되지 않은 항목입니다. (FINANCE_CASH_FLOW_NOT_MATCHED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 입출금 내역입니다. (FINANCE_CASH_FLOW_NOT_FOUND)")
    })
    @PatchMapping("/cash-flows/{cashFlowId}/unmatch")
    public ResponseEntity<ApiResponse<Void>> unmatchCashFlow(
            @Parameter(description = "매칭 해제할 입출금 내역 ID", example = "5") @PathVariable Long cashFlowId,
            Authentication authentication
    ) {
        financeCommandUseCase.unmatchCashFlow(
                new UnmatchCashFlowCommand(cashFlowId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("입출금 내역 블록 매칭 해제 성공", null));
    }

    @Operation(summary = "입출금 내역 직접 등록",
            description = "은행 CSV 업로드 없이 입출금 내역 한 건을 직접 등록한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "입출금 내역 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "필수 항목이 누락되었습니다. (FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING) / "
                            + "금액은 0보다 커야 합니다. (FINANCE_CASH_FLOW_AMOUNT_INVALID)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "이미 등록된 거래입니다. (FINANCE_CASH_FLOW_DUPLICATE)")
    })
    @PostMapping("/cash-flows")
    public ResponseEntity<ApiResponse<CashFlowCreateResponse>> createCashFlow(
            @RequestBody CreateCashFlowRequest request, Authentication authentication
    ) {
        CashFlowDetailView view = financeCommandUseCase.createCashFlow(
                request.toCommand(authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("입출금 내역 등록 성공", CashFlowCreateResponse.from(view)));
    }

    @Operation(summary = "입출금 내역 수정",
            description = "직접 등록한 입출금 내역을 수정한다. CSV/API 출처이거나 이미 정산 블록에 매칭된 항목은 메모만 수정할 수 있다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입출금 내역 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "필수 항목이 누락되었습니다. (FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING) / "
                            + "메모만 수정할 수 있습니다. (FINANCE_CASH_FLOW_FIELD_EDIT_NOT_ALLOWED) / "
                            + "금액은 0보다 커야 합니다. (FINANCE_CASH_FLOW_AMOUNT_INVALID)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 입출금 내역입니다. (FINANCE_CASH_FLOW_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "이미 등록된 거래입니다. (FINANCE_CASH_FLOW_DUPLICATE)")
    })
    @PatchMapping("/cash-flows/{cashFlowId}")
    public ResponseEntity<ApiResponse<CashFlowUpdateResponse>> updateCashFlow(
            @Parameter(description = "수정할 입출금 내역 ID", example = "20") @PathVariable Long cashFlowId,
            @RequestBody UpdateCashFlowRequest request,
            Authentication authentication
    ) {
        CashFlowDetailView view = financeCommandUseCase.updateCashFlow(
                request.toCommand(cashFlowId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("입출금 내역 수정 성공", CashFlowUpdateResponse.from(view)));
    }

    @Operation(summary = "입출금 내역 삭제",
            description = "선택한 입출금 내역을 일괄 삭제한다. 이미 정산 블록에 매칭된 항목은 건너뛰고 결과에 사유와 함께 알려준다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입출금 내역 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "필수 항목이 누락되었습니다. (FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING, cashFlowIds 비어있음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)")
    })
    @DeleteMapping("/cash-flows")
    public ResponseEntity<ApiResponse<CashFlowDeleteResponse>> deleteCashFlows(
            @RequestBody DeleteCashFlowsRequest request, Authentication authentication
    ) {
        CashFlowDeleteResultView view = financeCommandUseCase.deleteCashFlows(new DeleteCashFlowsCommand(
                request.cashFlowIds(), authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("입출금 내역 삭제 성공", CashFlowDeleteResponse.from(view)));
    }

    @Operation(summary = "입출금 내역 연결 제외 처리",
            description = "프로젝트와 무관한 입출금 내역을 미연결 건수 집계에서 빼거나(true), 다시 포함시킨다(false). "
                    + "제외 처리(true)는 이미 정산 블록에 매칭된 항목엔 적용되지 않는다 — 그런 항목은 건너뛰고 결과에 사유를 알려준다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입출금 내역 연결 제외 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "필수 항목이 누락되었습니다. (FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING, cashFlowIds/isExcluded 누락)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (FINANCE_EDIT_ACCESS_DENIED)")
    })
    @PatchMapping("/cash-flows/exclude")
    public ResponseEntity<ApiResponse<CashFlowExclusionResponse>> updateCashFlowExclusion(
            @RequestBody UpdateCashFlowExclusionRequest request, Authentication authentication
    ) {
        CashFlowExclusionResultView view = financeCommandUseCase.updateCashFlowExclusion(new UpdateCashFlowExclusionCommand(
                request.cashFlowIds(), request.isExcluded(),
                authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("입출금 내역 연결 제외 처리 성공", CashFlowExclusionResponse.from(view)));
    }

    private CashFlowCsvUploadRequest parseUploadRequest(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, "request가 필요합니다.");
        }
        try {
            return objectMapper.readValue(requestJson, CashFlowCsvUploadRequest.class);
        } catch (JsonProcessingException e) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, e);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE, e);
        }
    }
}

package com.group3.vitamins.finance.application.service;

import com.group3.vitamins.finance.application.port.PagePermissionPort;
import com.group3.vitamins.finance.application.query.CashFlowFilterQuery;
import com.group3.vitamins.finance.application.query.CashFlowListQuery;
import com.group3.vitamins.finance.application.query.FinanceSummaryQuery;
import com.group3.vitamins.finance.application.query.MatchCandidatesQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceFilterQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceListQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceMatchCandidatesQuery;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase;
import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowBasicRow;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowFilterProjectRow;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowMapper;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowRow;
import com.group3.vitamins.finance.infrastructure.cashflow.MatchCandidateRow;
import com.group3.vitamins.finance.infrastructure.status.FinanceSummaryMapper;
import com.group3.vitamins.finance.infrastructure.status.FinanceSummaryRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceBasicRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceFilterProjectRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceMapper;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceMatchCandidateRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceRow;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FinanceQueryService implements FinanceQueryUseCase {

    private static final String FINANCE_PAGE_CODE = "FINANCE";
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_TAX_INVOICE_SORTS =
            Set.of("ISSUED_NO_DESC", "ISSUED_NO_ASC", "AMOUNT_DESC");

    private final PagePermissionPort pagePermissionPort;
    private final FinanceSummaryMapper financeSummaryMapper;
    private final CashFlowMapper cashFlowMapper;
    private final TaxInvoiceMapper taxInvoiceMapper;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public FinanceSummaryView getSummary(FinanceSummaryQuery query) {
        log.info("재무 관리 요약 조회 요청 - userId={}", query.userId());

        assertFinanceAccess(query.userId(), query.role());

        FinanceSummaryRow row = financeSummaryMapper.findSummary(currentCompanyIdProvider.currentCompanyId());

        return new FinanceSummaryView(
                row.cashFlowUnlinkedCount(),
                row.cashFlowTotalCount(),
                row.taxInvoiceUnlinkedCount(),
                row.taxInvoiceTotalCount(),
                row.settlementUnlinkedCount(),
                row.settlementInProgressCount()
        );
    }

    @Override
    public CashFlowListView getCashFlows(CashFlowListQuery query) {
        log.info("입출금 내역 조회 요청 - userId={}", query.userId());

        assertFinanceAccess(query.userId(), query.role());

        List<CashFlowRow> rows = cashFlowMapper.findCashFlows(
                currentCompanyIdProvider.currentCompanyId(),
                query.startDate(), query.endDate(), query.unlinked(), query.projectId(), query.keyword());

        return new CashFlowListView(rows.stream().map(this::toCashFlowView).toList());
    }

    private CashFlowView toCashFlowView(CashFlowRow row) {
        return new CashFlowView(
                row.cashFlowId(),
                row.tradedAt(),
                row.bankTxnId(),
                row.type(),
                row.amount(),
                row.depositorName(),
                row.bankMemo(),
                row.sourceType(),
                row.projectId(),
                row.projectName(),
                row.settleId(),
                row.roundName(),
                row.linkedBy(),
                row.linkedByName(),
                row.linkedAt(),
                row.isExcluded(),
                row.linkStatus()
        );
    }

    @Override
    public CashFlowFilterView getCashFlowFilters(CashFlowFilterQuery query) {
        log.info("입출금 내역 필터 옵션 조회 요청 - userId={}", query.userId());

        assertFinanceAccess(query.userId(), query.role());

        List<CashFlowFilterProjectRow> rows =
                cashFlowMapper.findFilterProjects(currentCompanyIdProvider.currentCompanyId());

        return new CashFlowFilterView(rows.stream()
                .map(row -> new CashFlowProjectOptionView(row.projectId(), row.projectName()))
                .toList());
    }

    @Override
    public MatchCandidatesView getMatchCandidates(MatchCandidatesQuery query) {
        log.info("입출금 내역 매칭 추천 조회 요청 - cashFlowId={}, userId={}", query.cashFlowId(), query.userId());

        assertFinanceEditAccess(query.userId(), query.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        CashFlowBasicRow cashFlow = cashFlowMapper.findBasicById(query.cashFlowId(), companyId);
        if (cashFlow == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_CASH_FLOW_NOT_FOUND);
        }

        List<MatchCandidateRow> rows = cashFlowMapper.findMatchCandidates(
                cashFlow.type(), cashFlow.amount(), cashFlow.tradedAt().toLocalDate(), cashFlow.depositorName(),
                companyId);

        return new MatchCandidatesView(rows.stream().map(this::toMatchCandidateView).toList());
    }

    private MatchCandidateView toMatchCandidateView(MatchCandidateRow row) {
        List<String> matchTags = new ArrayList<>();
        addMatchTag(matchTags, row.amountMatchType(), "금액");
        addMatchTag(matchTags, row.dateMatchType(), "일자");
        addMatchTag(matchTags, row.traderMatchType(), "상호명");

        return new MatchCandidateView(
                row.settleId(), row.roundName(), row.projectName(),
                row.plannedAmount(), row.plannedDate(), row.traderName(), matchTags
        );
    }

    private void addMatchTag(List<String> matchTags, String matchType, String label) {
        if ("EXACT".equals(matchType)) {
            matchTags.add(label + " 일치");
        } else if ("SIMILAR".equals(matchType)) {
            matchTags.add(label + " 유사");
        }
    }

    @Override
    public TaxInvoiceListView getTaxInvoices(TaxInvoiceListQuery query) {
        log.info("세금계산서 조회 요청 - userId={}", query.userId());

        // 권한 검사가 파라미터 검증보다 먼저여야 한다 — 순서가 반대면 권한 없는 사용자가 잘못된 파라미터를
        // 같이 보냈을 때 403 대신 400이 나간다(정산현황 프로젝트 조회·입출금 내역 조회에서 이미 고친
        // 것과 동일한 버그, 2026-08-13).
        assertFinanceAccess(query.userId(), query.role());
        validateTaxInvoiceListQuery(query);

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        List<TaxInvoiceRow> rows = taxInvoiceMapper.findTaxInvoices(
                companyId, query.startDate(), query.endDate(), query.unlinked(), query.projectId(), query.keyword(),
                query.sort(), query.size(), query.page() * query.size());
        long totalElements = taxInvoiceMapper.countTaxInvoices(
                companyId, query.startDate(), query.endDate(), query.unlinked(), query.projectId(), query.keyword());
        int totalPages = (int) Math.ceil((double) totalElements / query.size());

        return new TaxInvoiceListView(
                rows.stream().map(this::toTaxInvoiceView).toList(),
                query.page(), query.size(), totalElements, totalPages
        );
    }

    private TaxInvoiceView toTaxInvoiceView(TaxInvoiceRow row) {
        return new TaxInvoiceView(
                row.taxId(),
                row.issuedNo(),
                row.approvalNo(),
                row.type(),
                row.buyerName(),
                row.buyerBizNo(),
                row.supplierBizNo(),
                row.subBizNo(),
                row.ceoName(),
                row.itemName(),
                row.supplyAmount(),
                row.taxAmount(),
                row.totalAmount(),
                row.memo(),
                row.sourceType(),
                row.projectId(),
                row.projectName(),
                row.settleId(),
                row.roundName(),
                row.linkedBy(),
                row.linkedByName(),
                row.linkedAt(),
                row.isExcluded(),
                row.linkStatus()
        );
    }

    // bidnotice 목록과 동일 컨벤션 — 잘못된 page/size/sort는 클램프 대신 400으로 던진다.
    private void validateTaxInvoiceListQuery(TaxInvoiceListQuery query) {
        if (query.page() < 0 || query.size() <= 0 || query.size() > MAX_PAGE_SIZE
                || query.page() > Integer.MAX_VALUE / query.size()
                || (query.startDate() != null && query.endDate() != null
                && query.startDate().isAfter(query.endDate()))
                || (query.sort() != null && !ALLOWED_TAX_INVOICE_SORTS.contains(query.sort()))) {
            throw new ValidationException(FinanceErrorCode.FINANCE_PAGE_QUERY_INVALID);
        }
    }

    @Override
    public TaxInvoiceFilterView getTaxInvoiceFilters(TaxInvoiceFilterQuery query) {
        log.info("세금계산서 필터 옵션 조회 요청 - userId={}", query.userId());

        assertFinanceAccess(query.userId(), query.role());

        List<TaxInvoiceFilterProjectRow> rows =
                taxInvoiceMapper.findFilterProjects(currentCompanyIdProvider.currentCompanyId());

        return new TaxInvoiceFilterView(rows.stream()
                .map(row -> new TaxInvoiceProjectOptionView(row.projectId(), row.projectName()))
                .toList());
    }

    @Override
    public TaxInvoiceMatchCandidatesView getTaxInvoiceMatchCandidates(TaxInvoiceMatchCandidatesQuery query) {
        log.info("세금계산서 매칭 추천 조회 요청 - taxId={}, userId={}", query.taxId(), query.userId());

        assertFinanceEditAccess(query.userId(), query.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        TaxInvoiceBasicRow taxInvoice = taxInvoiceMapper.findBasicById(query.taxId(), companyId);
        if (taxInvoice == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_TAX_INVOICE_NOT_FOUND);
        }

        List<TaxInvoiceMatchCandidateRow> rows = taxInvoiceMapper.findMatchCandidates(
                taxInvoice.type(), taxInvoice.totalAmount(), taxInvoice.taxAmount(), taxInvoice.issuedNo(),
                taxInvoice.buyerName(), companyId);

        return new TaxInvoiceMatchCandidatesView(rows.stream().map(this::toTaxInvoiceMatchCandidateView).toList());
    }

    private TaxInvoiceMatchCandidateView toTaxInvoiceMatchCandidateView(TaxInvoiceMatchCandidateRow row) {
        List<String> matchTags = new ArrayList<>();
        addMatchTag(matchTags, row.amountMatchType(), "금액");
        addMatchTag(matchTags, row.taxAmountMatchType(), "세액");
        addMatchTag(matchTags, row.traderMatchType(), "상호명");
        addMatchTag(matchTags, row.dateMatchType(), "일자");

        return new TaxInvoiceMatchCandidateView(
                row.settleId(), row.roundName(), row.projectName(),
                row.plannedAmount(), row.plannedTaxAmount(), row.plannedDate(), row.traderName(), matchTags
        );
    }

    private void assertFinanceAccess(String userId, String role) {
        if (!pagePermissionPort.hasAccess(FINANCE_PAGE_CODE, userId, role)) {
            log.warn("재무 관리 페이지 접근 권한 없음 - userId={}", userId);
            throw new ForbiddenException(FinanceErrorCode.FINANCE_ACCESS_DENIED);
        }
    }

    private void assertFinanceEditAccess(String userId, String role) {
        if (!pagePermissionPort.hasEditAccess(FINANCE_PAGE_CODE, userId, role)) {
            log.warn("재무 관리 페이지 편집 권한 없음 - userId={}", userId);
            throw new ForbiddenException(FinanceErrorCode.FINANCE_EDIT_ACCESS_DENIED);
        }
    }
}

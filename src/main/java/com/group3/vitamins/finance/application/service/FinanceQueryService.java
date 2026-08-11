package com.group3.vitamins.finance.application.service;

import com.group3.vitamins.finance.application.port.PagePermissionPort;
import com.group3.vitamins.finance.application.query.CashFlowFilterQuery;
import com.group3.vitamins.finance.application.query.CashFlowListQuery;
import com.group3.vitamins.finance.application.query.FinanceSummaryQuery;
import com.group3.vitamins.finance.application.query.MatchCandidatesQuery;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase;
import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowBasicRow;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowFilterProjectRow;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowMapper;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowRow;
import com.group3.vitamins.finance.infrastructure.cashflow.MatchCandidateRow;
import com.group3.vitamins.finance.infrastructure.status.FinanceSummaryMapper;
import com.group3.vitamins.finance.infrastructure.status.FinanceSummaryRow;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FinanceQueryService implements FinanceQueryUseCase {

    private static final String FINANCE_PAGE_CODE = "FINANCE";

    private final PagePermissionPort pagePermissionPort;
    private final FinanceSummaryMapper financeSummaryMapper;
    private final CashFlowMapper cashFlowMapper;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public FinanceSummaryView getSummary(FinanceSummaryQuery query) {
        log.info("재무 관리 요약 조회 요청 - userId={}", query.userId());

        assertFinanceAccess(query.userId(), query.role());

        FinanceSummaryRow row = financeSummaryMapper.findSummary();

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

        CashFlowBasicRow cashFlow =
                cashFlowMapper.findBasicById(query.cashFlowId(), currentCompanyIdProvider.currentCompanyId());
        if (cashFlow == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_CASH_FLOW_NOT_FOUND);
        }

        List<MatchCandidateRow> rows = cashFlowMapper.findMatchCandidates(
                cashFlow.type(), cashFlow.amount(), cashFlow.tradedAt().toLocalDate(), cashFlow.depositorName());

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

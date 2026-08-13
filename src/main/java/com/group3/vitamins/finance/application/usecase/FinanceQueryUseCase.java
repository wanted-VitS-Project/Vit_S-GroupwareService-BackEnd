package com.group3.vitamins.finance.application.usecase;

import com.group3.vitamins.finance.application.query.CashFlowFilterQuery;
import com.group3.vitamins.finance.application.query.CashFlowListQuery;
import com.group3.vitamins.finance.application.query.FinanceSummaryQuery;
import com.group3.vitamins.finance.application.query.MatchCandidatesQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceFilterQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceListQuery;
import com.group3.vitamins.finance.application.query.TaxInvoiceMatchCandidatesQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FinanceQueryUseCase {

    //재무 관리 페이지 진입 시 입출금·세금계산서·정산 현황 요약 조회
    FinanceSummaryView getSummary(FinanceSummaryQuery query);

    //입출금 내역 목록 조회
    CashFlowListView getCashFlows(CashFlowListQuery query);

    //입출금 내역 필터 옵션(매칭된 프로젝트 목록) 조회
    CashFlowFilterView getCashFlowFilters(CashFlowFilterQuery query);

    //입출금 내역 매칭 추천 조회
    MatchCandidatesView getMatchCandidates(MatchCandidatesQuery query);

    //세금계산서 목록 조회
    TaxInvoiceListView getTaxInvoices(TaxInvoiceListQuery query);

    //세금계산서 필터 옵션(매칭된 프로젝트 목록) 조회
    TaxInvoiceFilterView getTaxInvoiceFilters(TaxInvoiceFilterQuery query);

    //세금계산서 매칭 추천 조회
    TaxInvoiceMatchCandidatesView getTaxInvoiceMatchCandidates(TaxInvoiceMatchCandidatesQuery query);

    record FinanceSummaryView(
            long cashFlowUnlinkedCount,
            long cashFlowTotalCount,
            long taxInvoiceUnlinkedCount,
            long taxInvoiceTotalCount,
            long settlementUnlinkedCount,
            long settlementInProgressCount
    ) {
    }

    record CashFlowListView(List<CashFlowView> cashFlows) {
    }

    record CashFlowView(
            Long cashFlowId,
            LocalDateTime tradedAt,
            String bankTxnId,
            String type,
            BigDecimal amount,
            String depositorName,
            String bankMemo,
            String sourceType,
            Long projectId,
            String projectName,
            Long settleId,
            String roundName,
            String linkedBy,
            String linkedByName,
            LocalDateTime linkedAt,
            boolean isExcluded,
            String linkStatus
    ) {
    }

    record CashFlowFilterView(List<CashFlowProjectOptionView> projects) {
    }

    record CashFlowProjectOptionView(Long projectId, String projectName) {
    }

    record MatchCandidatesView(List<MatchCandidateView> candidates) {
    }

    record TaxInvoiceListView(
            List<TaxInvoiceView> taxInvoices, int page, int size, long totalElements, int totalPages
    ) {
    }

    record TaxInvoiceView(
            Long taxId,
            LocalDate issuedNo,
            String approvalNo,
            String type,
            String buyerName,
            String buyerBizNo,
            String supplierBizNo,
            String subBizNo,
            String ceoName,
            String itemName,
            BigDecimal supplyAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String memo,
            String sourceType,
            Long projectId,
            String projectName,
            Long settleId,
            String roundName,
            String linkedBy,
            String linkedByName,
            LocalDateTime linkedAt,
            boolean isExcluded,
            String linkStatus
    ) {
    }

    record TaxInvoiceFilterView(List<TaxInvoiceProjectOptionView> projects) {
    }

    record TaxInvoiceProjectOptionView(Long projectId, String projectName) {
    }

    record MatchCandidateView(
            Long settleId,
            String roundName,
            String projectName,
            BigDecimal plannedAmount,
            LocalDate plannedDate,
            String traderName,
            List<String> matchTags
    ) {
    }

    record TaxInvoiceMatchCandidatesView(List<TaxInvoiceMatchCandidateView> candidates) {
    }

    record TaxInvoiceMatchCandidateView(
            Long settleId,
            String roundName,
            String projectName,
            BigDecimal plannedAmount,
            BigDecimal plannedTaxAmount,
            LocalDate plannedDate,
            String traderName,
            List<String> matchTags
    ) {
    }
}

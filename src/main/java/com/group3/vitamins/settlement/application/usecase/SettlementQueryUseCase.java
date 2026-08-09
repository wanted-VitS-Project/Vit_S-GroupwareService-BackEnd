package com.group3.vitamins.settlement.application.usecase;

import com.group3.vitamins.settlement.application.query.SettlementFilterQuery;
import com.group3.vitamins.settlement.application.query.SettlementProjectBlockListQuery;
import com.group3.vitamins.settlement.application.query.SettlementProjectListQuery;
import com.group3.vitamins.settlement.application.query.SettlementRecommendationQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SettlementQueryUseCase {

    //정산 항목 수정 화면 진입 시 추천값(회차 번호·총 금액) + 원본 계좌번호 조회
    SettlementRecommendationView getRecommendation(SettlementRecommendationQuery query);

    //재무팀 정산현황 화면의 필터 옵션(발주처 목록) 조회
    SettlementFilterView getFilters(SettlementFilterQuery query);

    //재무팀 정산현황 화면의 프로젝트 단위 목록 조회
    SettlementProjectListView getProjectSettlements(SettlementProjectListQuery query);

    //한 프로젝트에 속한 정산 블록 회차별 내역 조회
    SettlementProjectBlockListView getProjectSettlementBlocks(SettlementProjectBlockListQuery query);

    //컨트롤러에 전달할 결과
    record SettlementRecommendationView(
            Long settleId,
            Integer recommendedRoundNo,
            Long recommendedTotalAmount,
            String originalAccountNumber
    ) {
    }

    record SettlementFilterView(List<String> clients) {
    }

    record SettlementProjectListView(List<SettlementProjectView> projects) {
    }

    record SettlementProjectView(
            Long projectId,
            String projectName,
            String clientName,
            String projectManager,
            Long totalPlannedAmount,
            Long totalOutcome,
            Long totalIncome,
            Long totalAmount,
            Integer completedRoundCount,
            Integer totalRoundCount,
            LocalDate nextPlannedDate,
            String settlementStatusSummary,
            String projectStatus,
            LocalDate endedOn
    ) {
    }

    record SettlementProjectBlockListView(List<SettlementProjectBlockView> blocks) {
    }

    record SettlementProjectBlockView(
            Long settleId,
            Integer roundNo,
            String roundName,
            LocalDate plannedDate,
            Long plannedAmount,
            Long plannedTaxAmount,
            LocalDate taxInvoiceDate,
            Long taxInvoiceAmount,
            String paidType,
            String bankName,
            String accountNumber,
            String accountHolder,
            LocalDate paidDate,
            Long paidAmount,
            String status,
            String taxLinkedBy,
            String taxLinkedByName,
            LocalDateTime taxLinkedAt,
            String cashFlowLinkedBy,
            String cashFlowLinkedByName,
            LocalDateTime cashFlowLinkedAt
    ) {
    }
}

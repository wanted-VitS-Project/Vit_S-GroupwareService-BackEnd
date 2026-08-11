package com.group3.vitamins.finance.application.usecase;

import com.group3.vitamins.finance.application.command.CashFlowCsvPreviewCommand;
import com.group3.vitamins.finance.application.command.CashFlowCsvUploadCommand;
import com.group3.vitamins.finance.application.command.CreateCashFlowCommand;
import com.group3.vitamins.finance.application.command.DeleteCashFlowsCommand;
import com.group3.vitamins.finance.application.command.MatchCashFlowCommand;
import com.group3.vitamins.finance.application.command.UnmatchCashFlowCommand;
import com.group3.vitamins.finance.application.command.UpdateCashFlowCommand;
import com.group3.vitamins.finance.application.command.UpdateCashFlowExclusionCommand;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FinanceCommandUseCase {

    //입출금 내역 CSV 업로드 전 컬럼 매핑 추천 조회
    CashFlowCsvPreviewView previewCashFlowCsv(CashFlowCsvPreviewCommand command);

    //확정된 컬럼 매핑으로 CSV를 파싱해 입출금 내역으로 저장
    CashFlowCsvUploadView uploadCashFlowCsv(CashFlowCsvUploadCommand command);

    //입출금 내역을 정산 블록에 매칭
    CashFlowMatchView matchCashFlow(MatchCashFlowCommand command);

    //입출금 내역의 정산 블록 매칭 해제
    void unmatchCashFlow(UnmatchCashFlowCommand command);

    //입출금 내역 직접 등록
    CashFlowDetailView createCashFlow(CreateCashFlowCommand command);

    //입출금 내역 수정
    CashFlowDetailView updateCashFlow(UpdateCashFlowCommand command);

    //입출금 내역 배치 삭제(매칭된 항목은 건너뜀)
    CashFlowDeleteResultView deleteCashFlows(DeleteCashFlowsCommand command);

    //입출금 내역 연결 제외 처리/해제(배치, 제외 시 매칭된 항목은 건너뜀)
    CashFlowExclusionResultView updateCashFlowExclusion(UpdateCashFlowExclusionCommand command);

    record CashFlowCsvPreviewView(
            List<String> columns,
            List<String> bankOptions,
            List<Map<String, String>> sampleRows,
            String recommendedDateTimeMode,
            String recommendedAmountMode,
            CashFlowCsvMappingView recommendedMapping
    ) {
    }

    record CashFlowCsvMappingView(
            String tradedDateTimeColumn,
            String tradedDateColumn,
            String tradedTimeColumn,
            String amountColumn,
            String typeColumn,
            String incomeAmountColumn,
            String outcomeAmountColumn,
            String memoColumn,
            String depositorColumn,
            String balanceColumn
    ) {
    }

    record CashFlowCsvUploadView(
            int totalRows,
            int savedCount,
            int duplicateCount,
            List<DuplicateRowView> duplicateRows
    ) {
    }

    record DuplicateRowView(
            LocalDateTime tradedAt,
            BigDecimal amount,
            String reason
    ) {
    }

    record CashFlowMatchView(
            Long cashFlowId,
            Long settleId,
            String roundName,
            String projectName,
            String linkedBy,
            String linkedByName,
            LocalDateTime linkedAt
    ) {
    }

    record CashFlowDetailView(
            Long cashFlowId,
            String bankTxnId,
            String bankName,
            LocalDateTime tradedAt,
            String type,
            BigDecimal amount,
            String depositorName,
            String memo,
            String sourceType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    record CashFlowDeleteResultView(
            int deletedCount,
            List<SkippedCashFlowView> skippedItems
    ) {
    }

    record CashFlowExclusionResultView(
            int updatedCount,
            List<SkippedCashFlowView> skippedItems
    ) {
    }

    record SkippedCashFlowView(
            Long cashFlowId,
            String reason
    ) {
    }
}

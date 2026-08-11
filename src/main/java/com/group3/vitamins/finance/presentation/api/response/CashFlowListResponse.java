package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.CashFlowListView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.CashFlowView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CashFlowListResponse(
        List<CashFlowItem> cashFlows
) {

    public static CashFlowListResponse from(CashFlowListView view) {
        return new CashFlowListResponse(view.cashFlows().stream().map(CashFlowItem::from).toList());
    }

    public record CashFlowItem(
            @Schema(description = "입출금 내역 ID", example = "1")
            Long cashFlowId,
            @Schema(description = "거래 일시", example = "2026-07-15T10:30:00")
            LocalDateTime tradedAt,
            @Schema(description = "거래고유번호", example = "신한-20260715103000")
            String bankTxnId,
            @Schema(description = "구분 (INCOME/OUTCOME)", example = "INCOME")
            String type,
            @Schema(description = "거래 금액", example = "30000000")
            BigDecimal amount,
            @Schema(description = "입금자명/수취인명", example = "(주)한국기술공사")
            String depositorName,
            @Schema(description = "적요/통장 메모", example = "선급금", nullable = true)
            String bankMemo,
            @Schema(description = "수집 출처 (MANUAL/CSV/API)", example = "CSV")
            String sourceType,
            @Schema(description = "연결된 프로젝트 ID. 미연결이거나 프로젝트 자체가 삭제됐으면 null", example = "1", nullable = true)
            Long projectId,
            @Schema(description = "연결 프로젝트명. 미연결이거나 프로젝트 자체가 삭제됐으면 null", example = "한강 생태교육 환경개선사업", nullable = true)
            String projectName,
            @Schema(description = "연결된 정산 블록 아이디. 미연결이면 null — 블록이 삭제돼도 값은 유지됨(linkStatus 참고)",
                    example = "10", nullable = true)
            Long settleId,
            @Schema(description = "연결된 정산 블록명. 미연결이면 null — 블록이 삭제돼도 값은 유지됨(linkStatus 참고)",
                    example = "1차 정산(선급 60%)", nullable = true)
            String roundName,
            @Schema(description = "매칭 처리자 사번. 미연결이면 null", example = "vitas-EMP004", nullable = true)
            String linkedBy,
            @Schema(description = "매칭 처리자 이름. 미연결이면 null", example = "김재무", nullable = true)
            String linkedByName,
            @Schema(description = "매칭 일시. 미연결이면 null", example = "2026-06-30T14:00:00", nullable = true)
            LocalDateTime linkedAt,
            @Schema(description = "연결 제외 여부", example = "false")
            boolean isExcluded,
            @Schema(description = "정산 블록 연결 상태 — UNLINKED(미연결)/LINKED(연결됨)/"
                    + "LINK_BLOCK_DELETED(연결됐던 정산 블록이 삭제됨). 원 명세엔 없던 필드(2026-08-10 추가)",
                    example = "LINKED")
            String linkStatus
    ) {

        public static CashFlowItem from(CashFlowView view) {
            return new CashFlowItem(
                    view.cashFlowId(), view.tradedAt(), view.bankTxnId(), view.type(), view.amount(),
                    view.depositorName(), view.bankMemo(), view.sourceType(), view.projectId(),
                    view.projectName(), view.settleId(), view.roundName(), view.linkedBy(),
                    view.linkedByName(), view.linkedAt(), view.isExcluded(), view.linkStatus()
            );
        }
    }
}

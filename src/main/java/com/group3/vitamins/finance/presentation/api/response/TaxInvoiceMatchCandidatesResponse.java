package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.TaxInvoiceMatchCandidateView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.TaxInvoiceMatchCandidatesView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TaxInvoiceMatchCandidatesResponse(
        @Schema(description = "매칭 추천 후보 목록(최대 5건, 매칭 개수 많은 순)")
        List<MatchCandidateItem> candidates
) {

    public static TaxInvoiceMatchCandidatesResponse from(TaxInvoiceMatchCandidatesView view) {
        return new TaxInvoiceMatchCandidatesResponse(view.candidates().stream().map(MatchCandidateItem::from).toList());
    }

    public record MatchCandidateItem(
            @Schema(description = "정산 블록 ID", example = "10")
            Long settleId,
            @Schema(description = "정산 블록명(회차명)", example = "1차 정산(선급 60%)")
            String roundName,
            @Schema(description = "프로젝트명", example = "한강 생태교육 환경개선사업")
            String projectName,
            @Schema(description = "예정 금액", example = "270000000")
            BigDecimal plannedAmount,
            @Schema(description = "예정 세금 금액", example = "27000000")
            BigDecimal plannedTaxAmount,
            @Schema(description = "예정일", example = "2026-05-10")
            LocalDate plannedDate,
            @Schema(description = "거래처명", example = "환경부")
            String traderName,
            @Schema(description = "추천 이유 태그", example = "[\"금액 일치\", \"상호명 일치\"]")
            List<String> matchTags
    ) {

        public static MatchCandidateItem from(TaxInvoiceMatchCandidateView view) {
            return new MatchCandidateItem(
                    view.settleId(), view.roundName(), view.projectName(),
                    view.plannedAmount(), view.plannedTaxAmount(), view.plannedDate(), view.traderName(), view.matchTags()
            );
        }
    }
}

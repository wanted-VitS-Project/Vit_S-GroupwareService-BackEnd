package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.MatchCandidateView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.MatchCandidatesView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowMatchCandidatesResponse(
        @Schema(description = "매칭 추천 후보 목록(최대 5건, 매칭 개수 많은 순)")
        List<MatchCandidateItem> candidates
) {

    public static CashFlowMatchCandidatesResponse from(MatchCandidatesView view) {
        return new CashFlowMatchCandidatesResponse(view.candidates().stream().map(MatchCandidateItem::from).toList());
    }

    public record MatchCandidateItem(
            @Schema(description = "정산 블록 ID", example = "11")
            Long settleId,
            @Schema(description = "정산 블록명(회차명)", example = "2차 기성(중도금)")
            String roundName,
            @Schema(description = "프로젝트명", example = "한강 생태교육 환경개선사업")
            String projectName,
            @Schema(description = "예정 금액", example = "90000000")
            BigDecimal plannedAmount,
            @Schema(description = "예정일", example = "2026-09-10")
            LocalDate plannedDate,
            @Schema(description = "거래처명", example = "환경부")
            String traderName,
            @Schema(description = "추천 이유 태그", example = "[\"금액 일치\", \"상호명 일치\"]")
            List<String> matchTags
    ) {

        public static MatchCandidateItem from(MatchCandidateView view) {
            return new MatchCandidateItem(
                    view.settleId(), view.roundName(), view.projectName(),
                    view.plannedAmount(), view.plannedDate(), view.traderName(), view.matchTags()
            );
        }
    }
}

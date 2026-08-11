package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.CashFlowFilterView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CashFlowFilterResponse(
        List<ProjectOption> projects
) {

    public static CashFlowFilterResponse from(CashFlowFilterView view) {
        return new CashFlowFilterResponse(view.projects().stream()
                .map(p -> new ProjectOption(p.projectId(), p.projectName()))
                .toList());
    }

    public record ProjectOption(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,
            @Schema(description = "프로젝트명", example = "한강 생태교육 환경개선사업")
            String projectName
    ) {
    }
}

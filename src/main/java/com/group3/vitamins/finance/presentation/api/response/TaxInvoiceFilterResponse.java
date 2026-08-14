package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.TaxInvoiceFilterView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TaxInvoiceFilterResponse(
        @Schema(description = "필터용 프로젝트 목록 — tax_invoice가 하나라도 연결된 정산 블록을 가진 프로젝트만")
        List<ProjectOption> projects
) {

    public static TaxInvoiceFilterResponse from(TaxInvoiceFilterView view) {
        return new TaxInvoiceFilterResponse(view.projects().stream()
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

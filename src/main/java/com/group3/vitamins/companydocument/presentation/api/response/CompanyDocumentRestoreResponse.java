package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentRestoreResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사내 문서 복구(§6) 응답")
public record CompanyDocumentRestoreResponse(
        @Schema(description = "문서 번호") Long companyDocumentId,
        @Schema(description = "표시명") String name,
        @Schema(description = "카테고리 enum", example = "FINANCE") String category
) {

    public static CompanyDocumentRestoreResponse from(CompanyDocumentRestoreResult r) {
        return new CompanyDocumentRestoreResponse(r.companyDocumentId(), r.name(), r.category());
    }
}

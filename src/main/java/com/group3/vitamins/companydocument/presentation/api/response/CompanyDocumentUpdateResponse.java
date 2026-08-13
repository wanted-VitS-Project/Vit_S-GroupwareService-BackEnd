package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentUpdateResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사내 문서 수정(§4) 응답")
public record CompanyDocumentUpdateResponse(
        @Schema(description = "문서 번호") Long companyDocumentId,
        @Schema(description = "표시명") String name,
        @Schema(description = "카테고리 enum", example = "FINANCE") String category
) {

    public static CompanyDocumentUpdateResponse from(CompanyDocumentUpdateResult r) {
        return new CompanyDocumentUpdateResponse(r.companyDocumentId(), r.name(), r.category());
    }
}

package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentDeleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사내 문서 삭제(§5) 응답")
public record CompanyDocumentDeleteResponse(
        @Schema(description = "문서 번호") Long companyDocumentId,
        @Schema(description = "삭제 시각 yyyy-MM-dd HH:mm:ss") String deletedAt
) {

    public static CompanyDocumentDeleteResponse from(CompanyDocumentDeleteResult r) {
        return new CompanyDocumentDeleteResponse(
                r.companyDocumentId(), CompanyDocumentDateTimeFormat.format(r.deletedAt()));
    }
}

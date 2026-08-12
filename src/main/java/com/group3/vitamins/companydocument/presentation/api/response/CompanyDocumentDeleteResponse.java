package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentDeleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "사내 문서 삭제(§5) 응답")
public record CompanyDocumentDeleteResponse(
        @Schema(description = "문서 번호") Long companyDocumentId,
        @Schema(description = "삭제 시각 yyyy-MM-dd HH:mm:ss") String deletedAt
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CompanyDocumentDeleteResponse from(CompanyDocumentDeleteResult r) {
        return new CompanyDocumentDeleteResponse(
                r.companyDocumentId(), r.deletedAt() == null ? null : r.deletedAt().format(FMT));
    }
}
